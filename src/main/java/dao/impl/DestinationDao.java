/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao.impl;

import config.db.DBConnection;
import java.sql.*;
import java.util.*;

/**
 *
 * @author USER
 */
public class DestinationDao {
    public Map<Integer, String> listAll() {
        Map<Integer, String> destinations = new LinkedHashMap<>();
        String sql = "SELECT destination_id, city + ', ' + country AS name FROM Destinations ORDER BY city ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                destinations.put(rs.getInt("destination_id"), rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return destinations;
    }
}
