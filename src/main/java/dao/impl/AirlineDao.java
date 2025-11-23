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
public class AirlineDao {
    public Map<Integer, String> listAll() {
        Map<Integer, String> airlines = new LinkedHashMap<>();
        String sql = "SELECT airline_id, name FROM Airlines WHERE active = 1 ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                airlines.put(rs.getInt("airline_id"), rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return airlines;
    }
}
