package dao.impl;

import model.Reservation;
import config.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDao {

    // Buscar reserva por PNR
    public Reservation findByPNR(String pnr) {
        String sql = "SELECT * FROM Reservations WHERE pnr = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToReservation(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Buscar reserva por ID de pasajero
    public Reservation findByPassengerId(int passengerId) {
        String sql = "SELECT * FROM Reservations WHERE passenger_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, passengerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToReservation(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Listar reservas por vuelo
    public List<Reservation> listByFlight(int flightId) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM Reservations WHERE flight_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, flightId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToReservation(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Insertar nueva reserva
    public boolean insert(Reservation r) {
        String sql = "INSERT INTO Reservations (pnr, passenger_id, flight_id, status, seat_preference, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet keys = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, r.getPnr());
            ps.setInt(2, r.getPassengerId());
            ps.setInt(3, r.getFlightId());
            ps.setString(4, r.getStatus());
            ps.setString(5, r.getSeatPreference());
            ps.setTimestamp(6, Timestamp.valueOf(r.getCreatedAt()));
            int affected = ps.executeUpdate();
            if (affected == 1) {
                keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    r.setId(keys.getInt(1));
                }
                conn.commit();
                return true;
            } else {
                if (conn != null) conn.rollback();
            }
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                // ignore
            }
            e.printStackTrace();
        } finally {
            try { if (keys != null) keys.close(); } catch (SQLException ex) {}
            try { if (ps != null) ps.close(); } catch (SQLException ex) {}
            try { if (conn != null) conn.close(); } catch (SQLException ex) {}
        }
        return false;
    }

    // Actualizar estado de reserva
    public boolean updateStatus(int id, String newStatus) {
        String sql = "UPDATE Reservations SET status = ? WHERE reservation_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setInt(2, id);
            int updated = ps.executeUpdate();
            if (updated == 1) {
                conn.commit();
                return true;
            } else {
                if (conn != null) conn.rollback();
            }
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (SQLException ex) {}
            try { if (conn != null) conn.close(); } catch (SQLException ex) {}
        }
        return false;
    }

    // Helper para mapear ResultSet a objeto Reservation
    private Reservation mapRowToReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("reservation_id"));
        r.setPnr(rs.getString("pnr"));
        r.setPassengerId(rs.getInt("passenger_id"));
        r.setFlightId(rs.getInt("flight_id"));
        r.setStatus(rs.getString("status"));
        r.setSeatPreference(rs.getString("seat_preference"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        return r;
    }
}
