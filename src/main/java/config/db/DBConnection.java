package config.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import config.app.ConfigManager;
import java.sql.Connection;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestor de conexiones a BD con HikariCP.
 * Pool de conexiones reutilizables para mejor performance.
 */
public class DBConnection {
    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
    private static HikariDataSource dataSource;

    static {
        try {
            initializeDataSource();
            logger.info("Pool de conexiones HikariCP inicializado correctamente.");
        } catch (Exception e) {
            logger.error("Error al inicializar pool de conexiones", e);
            throw new RuntimeException("No se pudo inicializar la conexión a base de datos", e);
        }
    }

    private DBConnection() {
        // Clase utilitaria, no instanciable
    }

    private static void initializeDataSource() {
        ConfigManager config = ConfigManager.getInstance();
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getDbUrl());
        hikariConfig.setUsername(config.getDbUser());
        hikariConfig.setPassword(config.getDbPassword());
        hikariConfig.setMaximumPoolSize(config.getDbPoolSize());
        hikariConfig.setMaxLifetime(config.getDbPoolMaxLifetime());
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setAutoCommit(false);
        hikariConfig.setLeakDetectionThreshold(60000);
        hikariConfig.setPoolName("CheckInDB-Pool");
        
        dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * Obtiene una conexión del pool.
     * IMPORTANTE: El llamador es responsable de cerrar la conexión.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource no inicializado");
        }
        return dataSource.getConnection();
    }

    /**
     * Obtiene el DataSource para transacciones complejas.
     */
    public static HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Cierra el pool de conexiones (llamar en shutdown de aplicación).
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Pool de conexiones cerrado.");
        }
    }
}

