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
     * Inserta un nuevo parámetro de configuración.
     */
    public boolean insertParametro(String nombre, String valor) {
        try {
            ConfigParameter p = new ConfigParameter();
            p.setName(nombre);
            p.setValue(valor);
            boolean ok = dao.insert(p);
            if (ok) logger.info("Parámetro insertado: {} = {}", nombre, valor);
            return ok;
        } catch (Exception e) {
            logger.error("Error al insertar parámetro: {}", nombre, e);
            return false;
        }
    }

    /**
     * Elimina un parámetro por nombre.
     */
    public boolean deleteParametro(String nombre) {
        try {
            boolean ok = dao.deleteByName(nombre);
            if (ok) logger.info("Parámetro eliminado: {}", nombre);
            return ok;
        } catch (Exception e) {
            logger.error("Error al eliminar parámetro: {}", nombre, e);
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

    /**
     * Retorna el parámetro completo si existe, o null.
     */
    public ConfigParameter findByNameSafe(String name) {
        try {
            return dao.findByName(name);
        } catch (Exception e) {
            logger.error("Error buscando parámetro: {}", name, e);
            return null;
        }
    }

    /**
     * Lee un parámetro como double, devolviendo un valor por defecto si no existe o no es numérico.
     */
    public double getParameterDouble(String name, double defaultValue) {
        String v = getParameterValue(name, null);
        if (v == null) return defaultValue;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            logger.warn("Valor de parámetro no numérico para {}: {} -> usando default {}", name, v, defaultValue);
            return defaultValue;
        }
    }
}
