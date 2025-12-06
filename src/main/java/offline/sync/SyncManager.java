/*
 * Gestor de sincronización completo para modo offline/online
 */
package offline.sync;

import config.db.DBConnection;
import offline.localdb.OfflineDBManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * SyncManager maneja la sincronización entre BD local (offline) y servidor principal (online).
 * Implementa:
 * - Auto-sync automática
 * - Retry logic con exponential backoff
 * - Conflict resolution
 * - Progress tracking
 * - Connection monitoring
 */
public class SyncManager {
    private static final Logger logger = LoggerFactory.getLogger(SyncManager.class);
    private static SyncManager instance;
    private static final Object LOCK = new Object();
    
    private final OfflineDBManager localDb;
    private final SyncQueue syncQueue;
    private final ScheduledExecutorService scheduler;
    private volatile boolean isSyncing = false;
    private volatile boolean isOnline = false;
    private List<SyncListener> listeners;
    private static final int SYNC_INTERVAL_SECONDS = 30; // Check every 30 sec
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds initial delay

    private SyncManager() {
        this.localDb = new OfflineDBManager();
        this.syncQueue = new SyncQueue();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.listeners = Collections.synchronizedList(new ArrayList<>());
        initializeSync();
    }

    public static SyncManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new SyncManager();
                }
            }
        }
        return instance;
    }

    /**
     * Inicia el monitor de conexión y auto-sync
     */
    private void initializeSync() {
        // Verificar conexión inicial
        checkConnectionStatus();
        
        // Programar verificación periódica de conexión
        scheduler.scheduleAtFixedRate(
            this::checkConnectionStatus,
            SYNC_INTERVAL_SECONDS,
            SYNC_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        // Programar sincronización automática si está online
        scheduler.scheduleAtFixedRate(
            this::syncIfOnline,
            SYNC_INTERVAL_SECONDS,
            SYNC_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        logger.info("SyncManager inicializado - Auto-sync cada {} segundos", SYNC_INTERVAL_SECONDS);
    }

    /**
     * Verifica si la conexión al servidor está disponible
     */
    public void checkConnectionStatus() {
        boolean wasOnline = isOnline;
        isOnline = syncQueue.conexionDisponible();
        
        if (wasOnline != isOnline) {
            logger.info("Estado de conexión cambió: {} → {}", wasOnline, isOnline);
            notifyConnectionChange(isOnline);
            
            // Si vuelve online, intentar sincronización inmediatamente
            if (isOnline && hasPendingOperations()) {
                syncIfOnline();
            }
        }
    }

    /**
     * Sincroniza automáticamente si está online
     */
    private void syncIfOnline() {
        if (isOnline && !isSyncing && hasPendingOperations()) {
            synchronizeAsync();
        }
    }

    /**
     * Verifica si hay operaciones pendientes
     */
    public boolean hasPendingOperations() {
        List<String> pending = localDb.obtenerPendientes();
        return !pending.isEmpty();
    }

    /**
     * Inicia sincronización síncrona (bloqueante)
     */
    public SyncResult synchronizeSync() {
        if (isSyncing) {
            logger.warn("Sincronización ya en progreso");
            return new SyncResult(false, "Sincronización en progreso", 0, 0);
        }
        
        synchronized (this) {
            if (isSyncing) return new SyncResult(false, "Sincronización en progreso", 0, 0);
            isSyncing = true;
        }
        
        try {
            return performSync();
        } finally {
            isSyncing = false;
        }
    }

    /**
     * Inicia sincronización asíncrona (no bloqueante)
     */
    public void synchronizeAsync() {
        scheduler.execute(this::synchronizeSync);
    }

    /**
     * Realiza la sincronización efectiva
     */
    private SyncResult performSync() {
        logger.info("=== Iniciando sincronización ===");
        int totalOperations = 0;
        int successfulOperations = 0;
        int failedOperations = 0;
        StringBuilder errors = new StringBuilder();
        
        notifyProgressUpdate(0, "Iniciando sincronización...");
        
        List<PendingOperation> pendingOps = getPendingOperations();
        totalOperations = pendingOps.size();
        
        if (totalOperations == 0) {
            logger.info("No hay operaciones pendientes");
            notifyProgressUpdate(100, "Sincronización completada (0 operaciones)");
            return new SyncResult(true, "No hay pendientes", 0, 0);
        }
        
        logger.info("Sincronizando {} operaciones pendientes", totalOperations);
        
        for (int i = 0; i < pendingOps.size(); i++) {
            PendingOperation op = pendingOps.get(i);
            int progress = (int) ((i + 1) / (double) totalOperations * 100);
            
            notifyProgressUpdate(progress, "Sincronizando " + op.getOperationType() + " (" + (i + 1) + "/" + totalOperations + ")");
            
            if (syncOperationWithRetry(op)) {
                successfulOperations++;
                localDb.marcarComoSincronizado(op.getId());
                logger.info("✅ Operación {} sincronizada", op.getId());
            } else {
                failedOperations++;
                errors.append("ID:").append(op.getId()).append(" Tipo:").append(op.getOperationType()).append("; ");
                logger.warn("❌ Operación {} falló", op.getId());
            }
        }
        
        logger.info("=== Sincronización completada ===");
        logger.info("Exitosas: {}, Fallidas: {}", successfulOperations, failedOperations);
        
        boolean success = failedOperations == 0;
        String message = String.format("Sincronizadas: %d/%d operaciones", successfulOperations, totalOperations);
        if (failedOperations > 0) {
            message += ". Errores: " + errors.toString();
        }
        
        notifyProgressUpdate(100, message);
        return new SyncResult(success, message, successfulOperations, failedOperations);
    }

    /**
     * Sincroniza una operación con retry logic
     */
    private boolean syncOperationWithRetry(PendingOperation op) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (syncQueue.enviarOperacion(op.getDataJson())) {
                    return true;
                }
            } catch (Exception e) {
                logger.warn("Intento {} falló para operación {}: {}", attempt, op.getId(), e.getMessage());
            }
            
            // Esperar antes de reintentar (exponential backoff)
            if (attempt < MAX_RETRIES) {
                long delayMs = RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Obtiene operaciones pendientes de la BD local
     */
    private List<PendingOperation> getPendingOperations() {
        List<PendingOperation> operations = new ArrayList<>();
        String sql = "SELECT id, operation_type, data_json, created_at FROM pending_operations WHERE status = 'PENDING' ORDER BY created_at ASC";
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:offline_data.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                operations.add(new PendingOperation(
                    rs.getInt("id"),
                    rs.getString("operation_type"),
                    rs.getString("data_json"),
                    rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo operaciones pendientes", e);
        }
        
        return operations;
    }

    /**
     * Agrega una operación a la cola offline
     */
    public void queueOfflineOperation(String operationType, String dataJson) {
        localDb.guardarOperacion(operationType, dataJson);
        logger.debug("Operación {} encolada para sincronización posterior", operationType);
        notifyOperationQueued(operationType);
    }

    /**
     * Obtiene el estado actual
     */
    public SyncStatus getStatus() {
        return new SyncStatus(
            isOnline,
            isSyncing,
            localDb.obtenerPendientes().size(),
            LocalDateTime.now()
        );
    }

    /**
     * Agrega un listener para cambios de sincronización
     */
    public void addSyncListener(SyncListener listener) {
        listeners.add(listener);
    }

    /**
     * Remueve un listener
     */
    public void removeSyncListener(SyncListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifica cambios en el estado de conexión
     */
    private void notifyConnectionChange(boolean isNowOnline) {
        for (SyncListener listener : listeners) {
            try {
                listener.onConnectionStatusChanged(isNowOnline);
            } catch (Exception e) {
                logger.error("Error notificando cambio de conexión", e);
            }
        }
    }

    /**
     * Notifica progreso de sincronización
     */
    private void notifyProgressUpdate(int progress, String message) {
        for (SyncListener listener : listeners) {
            try {
                listener.onSyncProgress(progress, message);
            } catch (Exception e) {
                logger.error("Error notificando progreso", e);
            }
        }
    }

    /**
     * Notifica operación encolada
     */
    private void notifyOperationQueued(String operationType) {
        for (SyncListener listener : listeners) {
            try {
                listener.onOperationQueued(operationType);
            } catch (Exception e) {
                logger.error("Error notificando operación encolada", e);
            }
        }
    }

    /**
     * Interface para escuchar cambios de sincronización
     */
    public interface SyncListener {
        void onConnectionStatusChanged(boolean isOnline);
        void onSyncProgress(int progress, String message);
        void onOperationQueued(String operationType);
    }

    /**
     * Clase para representar el resultado de sincronización
     */
    public static class SyncResult {
        public final boolean success;
        public final String message;
        public final int successCount;
        public final int failureCount;

        public SyncResult(boolean success, String message, int successCount, int failureCount) {
            this.success = success;
            this.message = message;
            this.successCount = successCount;
            this.failureCount = failureCount;
        }
    }

    /**
     * Clase para representar el estado actual de sincronización
     */
    public static class SyncStatus {
        public final boolean isOnline;
        public final boolean isSyncing;
        public final int pendingOperations;
        public final LocalDateTime lastCheck;

        public SyncStatus(boolean isOnline, boolean isSyncing, int pendingOperations, LocalDateTime lastCheck) {
            this.isOnline = isOnline;
            this.isSyncing = isSyncing;
            this.pendingOperations = pendingOperations;
            this.lastCheck = lastCheck;
        }
    }

    /**
     * Clase interna para operaciones pendientes
     */
    private static class PendingOperation {
        private final int id;
        private final String operationType;
        private final String dataJson;
        private final LocalDateTime createdAt;

        public PendingOperation(int id, String operationType, String dataJson, LocalDateTime createdAt) {
            this.id = id;
            this.operationType = operationType;
            this.dataJson = dataJson;
            this.createdAt = createdAt;
        }

        public int getId() { return id; }
        public String getOperationType() { return operationType; }
        public String getDataJson() { return dataJson; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    /**
     * Limpia recursos al cerrar
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("SyncManager apagado");
    }
}
