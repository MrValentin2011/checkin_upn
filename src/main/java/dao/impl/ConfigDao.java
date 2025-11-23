/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao.impl;

import config.db.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.ConfigParameter;

/**
 *
 * @author USER
 */
public class ConfigDao {

    /**
     * Lista todos los parámetros de configuración del sistema.
     */
    public List<ConfigParameter> listAll() {
        List<ConfigParameter> list = new ArrayList<>();
        String sql = "SELECT key_name, value FROM Configurations ORDER BY key_name";

        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ConfigParameter c = new ConfigParameter();
                c.setName(rs.getString("key_name"));
                c.setValue(rs.getString("value"));
                list.add(c);
            }

        } catch (SQLException e) {
            System.err.println("Error al listar configuraciones: " + e.getMessage());
        }
        return list;
    }

    /**
     * Obtiene un parámetro específico por nombre (key_name).
     */
    public ConfigParameter findByName(String keyName) {
        String sql = "SELECT key_name, value FROM Configurations WHERE key_name = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, keyName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new ConfigParameter(0, rs.getString("key_name"), rs.getString("value"));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener configuración: " + e.getMessage());
        }
        return null;
    }

    /**
     * Actualiza el valor de una clave existente.
     */
    public boolean updateValue(String keyName, String value) {
        String sql = "UPDATE Configurations SET value = ? WHERE key_name = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, value);
            ps.setString(2, keyName);
            int updated = ps.executeUpdate();
            conn.commit();
            return updated > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar configuración: " + e.getMessage());
        }
        return false;
    }

    /**
     * Inserta una nueva configuración.
     */
    public boolean insert(ConfigParameter config) {
        String sql = "INSERT INTO Configurations (key_name, value) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, config.getName());
            ps.setString(2, config.getValue());
            int inserted = ps.executeUpdate();
            conn.commit();
            return inserted > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar configuración: " + e.getMessage());
        }
        return false;
    }

    /**
     * Elimina una configuración por su key_name.
     */
    public boolean deleteByName(String keyName) {
        String sql = "DELETE FROM Configurations WHERE key_name = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, keyName);
            int deleted = ps.executeUpdate();
            conn.commit();
            return deleted > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar configuración: " + e.getMessage());
        }
        return false;
    }
}
