/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package offline.localdb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author USER
 */
public class OfflineDBManager {

    private static final String DB_URL = "jdbc:sqlite:offline_data.db";

    public OfflineDBManager() {
        crearEstructuraLocal();
    }

    /**
     * Crea las tablas locales necesarias si no existen.
     */
    private void crearEstructuraLocal() {
        String sql = """
            CREATE TABLE IF NOT EXISTS pending_operations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                operation_type TEXT NOT NULL,
                data_json TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING'
            );
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creando estructura local: " + e.getMessage());
        }
    }

    /**
     * Guarda una operación pendiente (por ejemplo, un check-in o equipaje).
     *
     * @param operationType Tipo de operación (ej: "CHECKIN", "BAGGAGE", etc.)
     * @param dataJson Datos en formato JSON
     */
    public void guardarOperacion(String operationType, String dataJson) {
        String sql = "INSERT INTO pending_operations (operation_type, data_json) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, operationType);
            ps.setString(2, dataJson);
            ps.executeUpdate();
            System.out.println("✅ Operación guardada localmente: " + operationType);
        } catch (SQLException e) {
            System.err.println("❌ Error al guardar operación offline: " + e.getMessage());
        }
    }

    /**
     * Obtiene todas las operaciones pendientes de sincronizar.
     *
     * @return Lista de operaciones como JSON strings.
     */
    public List<String> obtenerPendientes() {
        List<String> pendientes = new ArrayList<>();
        String sql = "SELECT data_json FROM pending_operations WHERE status = 'PENDING'";
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                pendientes.add(rs.getString("data_json"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener pendientes: " + e.getMessage());
        }
        return pendientes;
    }

    /**
     * Marca una operación como sincronizada correctamente.
     *
     * @param dataJson Identificador (JSON exacto o hash) de la operación
     * sincronizada.
     */
    public void marcarComoSincronizado(String dataJson) {
        String sql = "UPDATE pending_operations SET status = 'SYNCED' WHERE data_json = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataJson);
            ps.executeUpdate();
            System.out.println("✅ Operación marcada como sincronizada.");
        } catch (SQLException e) {
            System.err.println("Error al marcar como sincronizado: " + e.getMessage());
        }
    }

    /**
     * Limpia registros antiguos sincronizados para mantener ligera la base
     * local.
     */
    public void limpiarSincronizados() {
        String sql = "DELETE FROM pending_operations WHERE status = 'SYNCED'";
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            System.out.println("🧹 Limpieza: " + deleted + " registros sincronizados eliminados.");
        } catch (SQLException e) {
            System.err.println("Error al limpiar operaciones sincronizadas: " + e.getMessage());
        }
    }
}
