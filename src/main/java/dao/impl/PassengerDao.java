package dao.impl;

import config.db.DBConnection;
import model.Passenger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PassengerDao {

    // ====== READ ======

    public Passenger findById(int id) {
        String sql = "SELECT * FROM Passengers WHERE passenger_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Passenger findByDocument(String docNumber) {
        String sql = "SELECT * FROM Passengers WHERE doc_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, docNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Lista completa ordenada por Apellido, Nombre */
    public List<Passenger> listAll() {
        List<Passenger> list = new ArrayList<>();
        String sql = "SELECT * FROM Passengers ORDER BY last_name, first_name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** Búsqueda por nombre/apellido/documento (LIKE) */
    public List<Passenger> search(String query) {
        List<Passenger> list = new ArrayList<>();
        String sql = """
            SELECT * FROM Passengers
            WHERE (? = '' OR first_name LIKE ? OR last_name LIKE ? OR doc_number LIKE ?)
            ORDER BY last_name, first_name
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + (query == null ? "" : query.trim()) + "%";
            String q = query == null ? "" : query.trim();
            ps.setString(1, q);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ====== CREATE / UPDATE / DELETE ======

    public boolean insert(Passenger p) {
        String sql = """
            INSERT INTO Passengers
              (first_name, last_name, doc_type, doc_number, dob, email, phone, frequent_counter)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());
            ps.setString(3, p.getDocumentType());
            ps.setString(4, p.getDocumentNumber());
            if (p.getDateOfBirth() != null) ps.setDate(5, Date.valueOf(p.getDateOfBirth())); else ps.setNull(5, Types.DATE);
            ps.setString(6, p.getEmail());
            ps.setString(7, p.getPhone());
            ps.setInt(8, p.getFrequentCounter());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) p.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Passenger p) {
        String sql = """
            UPDATE Passengers
               SET first_name = ?, last_name = ?, doc_type = ?, doc_number = ?,
                   dob = ?, email = ?, phone = ?, frequent_counter = ?
             WHERE passenger_id = ?
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());
            ps.setString(3, p.getDocumentType());
            ps.setString(4, p.getDocumentNumber());
            if (p.getDateOfBirth() != null) ps.setDate(5, Date.valueOf(p.getDateOfBirth())); else ps.setNull(5, Types.DATE);
            ps.setString(6, p.getEmail());
            ps.setString(7, p.getPhone());
            ps.setInt(8, p.getFrequentCounter());
            ps.setInt(9, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Elimina por ID. OJO: puede fallar por FK si tiene Reservas. */
    public boolean delete(int id) {
        String sql = "DELETE FROM Passengers WHERE passenger_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // por ejemplo, violación de FK con Reservations
            System.err.println("⚠️ No se pudo eliminar pasajero ID " + id + ": " + e.getMessage());
        }
        return false;
    }

    // ====== UTILIDADES ======

    /** Verifica si ya existe un documento (excluyendo un ID en edición). */
    public boolean existsByDocument(String docNumber, Integer excludePassengerId) {
        String sql = "SELECT COUNT(1) AS cnt FROM Passengers WHERE doc_number = ? " +
                     (excludePassengerId != null ? "AND passenger_id <> ?" : "");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, docNumber);
            if (excludePassengerId != null) ps.setInt(2, excludePassengerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt") > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Passenger map(ResultSet rs) throws SQLException {
        Passenger p = new Passenger();
        p.setId(rs.getInt("passenger_id"));
        p.setFirstName(rs.getString("first_name"));
        p.setLastName(rs.getString("last_name"));
        p.setDocumentType(rs.getString("doc_type"));
        p.setDocumentNumber(rs.getString("doc_number"));
        Date dob = rs.getDate("dob");
        if (dob != null) p.setDateOfBirth(dob.toLocalDate());
        p.setEmail(rs.getString("email"));
        p.setPhone(rs.getString("phone"));
        p.setFrequentCounter(rs.getInt("frequent_counter"));
        return p;
    }
}
