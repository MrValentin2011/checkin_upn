/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service.impl;

import dao.impl.ConfigDao;
import java.util.List;
import model.ConfigParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio de configuración con Singleton.
 */
public class ConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private final ConfigDao dao = new ConfigDao();
    private static ConfigService instance;

    private ConfigService() {
    }

    public static ConfigService getInstance() {
        if (instance == null) {
            synchronized (ConfigService.class) {
                if (instance == null) {
                    instance = new ConfigService();
                }
            }
        }
        return instance;
    }

    public List<ConfigParameter> listarParametros() {
        try {
            List<ConfigParameter> params = dao.listAll();
            logger.info("Parámetros de configuración cargados: {}", params.size());
            return params;
        } catch (Exception e) {
            logger.error("Error al listar parámetros de configuración", e);
            return List.of();
        }
    }

    public boolean actualizarParametro(String nombre, String valor) {
        try {
            boolean result = dao.updateValue(nombre, valor);
            if (result) {
                logger.info("Parámetro actualizado: {} = {}", nombre, valor);
            } else {
                logger.warn("Fallo al actualizar parámetro: {}", nombre);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error al actualizar parámetro: {}", nombre, e);
            return false;
        }
    }

    /**
     * Obtiene el valor de un parámetro; si no existe devuelve el valor por defecto.
     */
    public String getParameterValue(String name, String defaultValue) {
        try {
            var p = dao.findByName(name);
            if (p != null) return p.getValue();
        } catch (Exception e) {
            logger.error("Error al obtener parámetro: {}", name, e);
        }
        return defaultValue;
    }
}
