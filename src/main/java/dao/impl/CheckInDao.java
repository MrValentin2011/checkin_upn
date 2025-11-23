package dao.impl;

import config.db.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.sql.Types;

/**
 * Data Access Object para Check-ins.
 * Inserta y consulta check-ins con transacciones ACID y manejo de race conditions.
 */
public class CheckInDao {
    private static final Logger logger = LoggerFactory.getLogger(CheckInDao.class);

    public boolean existsByReservation(int reservationId) {
        String sql = "SELECT COUNT(1) AS cnt FROM CheckIns WHERE reservation_id = ? AND status = 'Checked-in'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean exists = rs.getInt("cnt") > 0;
                    conn.commit();
                    return exists;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al verificar existencia de check-in para reservación: {}", reservationId, e);
        }
        return false;
    }

    /**
     * Inserta un check-in con serialización completa (evita race conditions).
     * Usa nivel de aislamiento SERIALIZABLE.
     */
    public int insert(int reservationId, int agentUserId, Integer seatId, String boardingPassCode) throws SQLException {
        String insertSql = "INSERT INTO CheckIns (reservation_id, agent_user_id, checkin_time, seat_id, boarding_pass_code, status, synced) VALUES (?, ?, GETDATE(), ?, ?, 'Checked-in', 0)";
        
        try (Connection conn = DBConnection.getConnection()) {
            // Establecer nivel de aislamiento SERIALIZABLE para evitar race conditions
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, reservationId);
                ps.setInt(2, agentUserId);
                if (seatId != null) {
                    ps.setInt(3, seatId);
                } else {
                    ps.setNull(3, Types.INTEGER);
                }
                ps.setString(4, boardingPassCode);
                
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    logger.warn("Falló la inserción de check-in para reservación: {}", reservationId);
                    return -1;
                }
                try (ResultSet gen = ps.getGeneratedKeys()) {
                    if (gen.next()) {
                        int newId = gen.getInt(1);
                        conn.commit();
                        logger.info("Check-in insertado exitosamente. CheckInID: {}, ReservationID: {}", newId, reservationId);
                        return newId;
                    } else {
                        conn.rollback();
                        logger.error("No se pudo obtener el ID del check-in insertado (generated keys vacías)");
                        throw new SQLException("No se pudo obtener el ID del check-in insertado (generated keys vacías)");
                    }
                }
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (SQLException r) { /* ignore */ }
                logger.error("Error durante transacción de check-in para reservación: {}. SQL: {}. Mensaje: {}", reservationId, insertSql, ex.getMessage(), ex);
                ex.printStackTrace();
                throw ex;
            }
        }
    }

    /**
     * Obtiene un check-in por ID.
     */
    public CheckInData getById(int checkInId) {
        String sql = "SELECT checkin_id, reservation_id, agent_user_id, checkin_time, seat_id, boarding_pass_code, status, synced FROM CheckIns WHERE checkin_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, checkInId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    conn.commit();
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener check-in por ID: {}", checkInId, e);
        }
        return null;
    }

    private CheckInData mapResultSet(ResultSet rs) throws SQLException {
        CheckInData data = new CheckInData();
        data.checkInId = rs.getInt("checkin_id");
        data.reservationId = rs.getInt("reservation_id");
        data.agentUserId = rs.getInt("agent_user_id");
        data.checkInTime = rs.getTimestamp("checkin_time");
        data.seatId = rs.getObject("seat_id") != null ? rs.getInt("seat_id") : null;
        data.boardingPassCode = rs.getString("boarding_pass_code");
        data.status = rs.getString("status");
        data.synced = rs.getBoolean("synced");
        return data;
    }

    /**
     * DTO para datos de check-in.
     */
    public static class CheckInData {
        public int checkInId;
        public int reservationId;
        public int agentUserId;
        public Timestamp checkInTime;
        public Integer seatId;
        public String boardingPassCode;
        public String status;
        public boolean synced;
    }
}
