package config.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Gestor centralizado de configuración de la aplicación.
 * Carga propiedades desde config.properties y variables de entorno.
 */
public class ConfigManager {
    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());
    private static final Properties properties = new Properties();
    private static ConfigManager instance;

    static {
        try (InputStream input = ConfigManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                LOGGER.warning("No se encontró config.properties. Usando valores por defecto.");
            } else {
                properties.load(input);
                LOGGER.info("config.properties cargado exitosamente.");
            }
        } catch (IOException e) {
            LOGGER.severe("Error al cargar config.properties: " + e.getMessage());
        }
    }

    private ConfigManager() {
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Obtiene un valor de configuración con soporte a variables de entorno.
     * Prioridad: Variable de entorno > config.properties > valor por defecto
     */
    public String getProperty(String key, String defaultValue) {
        String envValue = System.getenv(key.replace(".", "_").toUpperCase());
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return properties.getProperty(key, defaultValue);
    }

    // Métodos específicos para configuración BD
    public String getDbUrl() {
        return getProperty("db.url", "jdbc:sqlserver://localhost:1433;databaseName=CheckInDB");
    }

    public String getDbUser() {
        return getProperty("db.user", "sa");
    }

    public String getDbPassword() {
        return getProperty("db.password", "");
    }

    public int getDbPoolSize() {
        return Integer.parseInt(getProperty("db.pool.size", "10"));
    }

    public long getDbPoolMaxLifetime() {
        return Long.parseLong(getProperty("db.pool.maxLifetime", "1800000"));
    }

    // Métodos para configuración de aplicación
    public String getAppName() {
        return getProperty("app.name", "AEROCHECK");
    }

    public String getAppVersion() {
        return getProperty("app.version", "1.0.0");
    }

    public String getLoggingLevel() {
        return getProperty("logging.level", "INFO");
    }

    public String getLoggingFilePath() {
        return getProperty("logging.file.path", "logs/");
    }

    public long getSessionTimeout() {
        return Long.parseLong(getProperty("session.timeout", "3600000"));
    }

    public boolean isAuditEnabled() {
        return Boolean.parseBoolean(getProperty("audit.enabled", "true"));
    }
}
