/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package offline.sync;

import config.db.DBConnection;
import java.sql.*;

/**
 *
 * @author USER
 */
public class SyncQueue {
    /**
     * Intenta enviar una operación JSON al servidor principal.
     * @param jsonData Contenido de la operación en formato JSON
     * @return true si se envió y guardó correctamente, false si falla
     */
    public boolean enviarOperacion(String jsonData) {
        // Aquí simulamos que el servidor SQL Server tiene una tabla de auditoría o sincronización.
        String sql = "INSERT INTO SyncLog (json_data, sync_date) VALUES (?, GETDATE())";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, jsonData);
            ps.executeUpdate();
            System.out.println("✅ Operación sincronizada correctamente con el servidor.");
            return true;

        } catch (SQLException e) {
            System.err.println("⚠️ Error al sincronizar con el servidor: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si la conexión al servidor principal está activa.
     * @return true si se puede conectar a SQL Server, false si no.
     */
    public boolean conexionDisponible() {
        try (Connection conn = DBConnection.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("No se pudo conectar al servidor principal: " + e.getMessage());
            return false;
        }
    }
}
