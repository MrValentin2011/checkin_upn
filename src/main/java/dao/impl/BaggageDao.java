/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao.impl;

import config.db.DBConnection;
import java.sql.*;
import java.util.*;
import model.Baggage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Access Object para Baggage.
 * Baggage está vinculado a CheckIns, no a Passengers directamente.
 */
public class BaggageDao {
    private static final Logger logger = LoggerFactory.getLogger(BaggageDao.class);

    public boolean insert(Baggage b) {
        // Baggage se inserta para un check-in, no para un pasajero
        String sql = "INSERT INTO Baggage (checkin_id, tag_code, weight, pieces, type) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, b.getCheckInId()); // CheckIn ID, no Passenger ID
            ps.setString(2, b.getTagCode());
            ps.setDouble(3, b.getWeight());
            ps.setInt(4, b.getPieces());
            ps.setString(5, b.getType());
            boolean result = ps.executeUpdate() > 0;
            if (result) {
                conn.commit();
            }
            return result;
        } catch (SQLException e) {
            logger.error("Error al insertar equipaje", e);
        }
        return false;
    }

    public List<Baggage> listByCheckIn(int checkInId) {
        List<Baggage> list = new ArrayList<>();
        String sql = "SELECT baggage_id, checkin_id, tag_code, weight, pieces, type, created_at FROM Baggage WHERE checkin_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, checkInId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            conn.commit();
        } catch (SQLException e) {
            logger.error("Error al listar equipaje por check-in", e);
        }
        return list;
    }

    private Baggage map(ResultSet rs) throws SQLException {
        Baggage b = new Baggage();
        b.setId(rs.getInt("baggage_id"));
        b.setCheckInId(rs.getInt("checkin_id"));
        b.setTagCode(rs.getString("tag_code"));
        b.setWeight(rs.getDouble("weight"));
        b.setPieces(rs.getInt("pieces"));
        b.setType(rs.getString("type"));
        return b;
    }
}
