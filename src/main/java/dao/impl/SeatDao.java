package dao.impl;

import config.db.DBConnection;
import model.Seat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object para Seats.
 * Operaciones sobre la tabla Seats con protección contra race conditions.
 */
public class SeatDao {
    private static final Logger logger = LoggerFactory.getLogger(SeatDao.class);

    public boolean bulkCreateSeatsByClass(Connection conn, int flightId, LinkedHashMap<String, Integer> classDistribution, int seatsPerRow) throws SQLException {
        String cols = seatLetters(seatsPerRow); // "ABCDEF" si 6
        String sql = "INSERT INTO Seats (flight_id, seat_code, class, occupied, reservation_id) VALUES (?, ?, ?, 0, NULL)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int row = 1;
            for (Map.Entry<String, Integer> e : classDistribution.entrySet()) {
                String seatClass = e.getKey();
                int count = e.getValue() != null ? e.getValue() : 0;
                if (count <= 0) {
                    continue;
                }

                int created = 0;
                while (created < count) {
                    for (int i = 0; i < cols.length() && created < count; i++) {
                        String code = row + String.valueOf(cols.charAt(i)); // 1A, 1B...
                        ps.setInt(1, flightId);
                        ps.setString(2, code);
                        ps.setString(3, seatClass);
                        ps.addBatch();
                        created++;
                    }
                    row++;
                }
            }
            ps.executeBatch();
            logger.info("Asientos creados en lote para vuelo: {}", flightId);
            return true;
        } catch (SQLException e) {
            logger.error("Error al crear asientos en lote para vuelo: {}", flightId, e);
            throw e;
        }
    }

    private String seatLetters(int seatsPerRow) {
        final String base = "ABCDEFGHJK"; // sin I para evitar confusión con 1
        if (seatsPerRow < 1 || seatsPerRow > base.length()) {
            throw new IllegalArgumentException("seatsPerRow inválido (1-" + base.length() + ")");
        }
        return base.substring(0, seatsPerRow);
    }

    public List<Seat> listAvailableSeatsByFlight(int flightId) {
        List<Seat> list = new ArrayList<>();
        String sql = "SELECT seat_id, flight_id, seat_code, class, occupied, reservation_id FROM Seats WHERE flight_id = ? AND occupied = 0 ORDER BY seat_code";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            ps.setInt(1, flightId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seat s = new Seat();
                    s.setId(rs.getInt("seat_id"));
                    s.setFlightId(rs.getInt("flight_id"));
                    s.setSeatCode(rs.getString("seat_code"));
                    s.setSeatClass(rs.getString("class"));
                    s.setOccupied(rs.getBoolean("occupied"));
                    int resId = rs.getInt("reservation_id");
                    s.setReservationId(rs.wasNull() ? null : resId);
                    list.add(s);
                }
            }
            conn.commit();
        } catch (SQLException e) {
            logger.error("Error al listar asientos disponibles para vuelo: {}", flightId, e);
        }
        return list;
    }

    /**
     * Lista todos los asientos (ocupados y libres) para un vuelo.
     */
    public List<Seat> listSeatsByFlight(int flightId) {
        List<Seat> list = new ArrayList<>();
        String sql = "SELECT seat_id, flight_id, seat_code, class, occupied, reservation_id FROM Seats WHERE flight_id = ? ORDER BY seat_code";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, flightId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seat s = new Seat();
                    s.setId(rs.getInt("seat_id"));
                    s.setFlightId(rs.getInt("flight_id"));
                    s.setSeatCode(rs.getString("seat_code"));
                    s.setSeatClass(rs.getString("class"));
                    s.setOccupied(rs.getBoolean("occupied"));
                    int resId = rs.getInt("reservation_id");
                    s.setReservationId(rs.wasNull() ? null : resId);
                    list.add(s);
                }
            }
            conn.commit();
        } catch (SQLException e) {
            logger.error("Error al listar asientos para vuelo: {}", flightId, e);
        }
        return list;
    }

    /**
     * Intenta asignar un asiento de forma segura.
     * Usa transacción SERIALIZABLE para evitar race conditions.
     * Devuelve true solo si la actualización afectó 1 fila.
     */
    public boolean assignSeat(int seatId, int reservationId) {
        String sql = "UPDATE Seats SET occupied = 1, reservation_id = ? WHERE seat_id = ? AND occupied = 0";
        try (Connection conn = DBConnection.getConnection()) {
            // Aislamiento máximo para evitar lecturas fantasma
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, reservationId);
                ps.setInt(2, seatId);
                int rowsAffected = ps.executeUpdate();
                
                if (rowsAffected > 0) {
                    conn.commit();
                    logger.info("Asiento {} asignado exitosamente a reservación: {}", seatId, reservationId);
                    return true;
                } else {
                    conn.rollback();
                    logger.warn("No se pudo asignar asiento {} - posiblemente ya ocupado", seatId);
                    return false;
                }
            } catch (SQLException ex) {
                conn.rollback();
                logger.error("Error al asignar asiento: {}", seatId, ex);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error en obtención de conexión para asignar asiento", e);
            return false;
        }
    }

    /**
     * Obtiene distribución de asientos por clase para un vuelo.
     */
    public Map<String, Integer> getClassCountsByFlight(int flightId) {
        String sql = "SELECT class, COUNT(*) AS cnt FROM Seats WHERE flight_id = ? GROUP BY class";
        Map<String, Integer> res = new java.util.HashMap<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, flightId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.put(rs.getString("class"), rs.getInt("cnt"));
                }
            }
            conn.commit();
        } catch (Exception e) {
            logger.error("Error al obtener conteo de asientos por clase para vuelo: {}", flightId, e);
        }
        return res;
    }

    /**
     * Infiere asientos por fila mirando la primera fila con asientos.
     */
    public Integer inferSeatsPerRow(int flightId) {
        String sql = "SELECT seat_code FROM Seats WHERE flight_id = ? ORDER BY seat_id ASC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, flightId);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.Set<Character> lettersFirstRow = new java.util.LinkedHashSet<>();
                int firstRowNumber = -1;

                while (rs.next()) {
                    String code = rs.getString("seat_code");
                    if (code == null || code.isBlank()) continue;
                    code = code.trim();

                    int i = code.length() - 1;
                    while (i >= 0 && Character.isLetter(code.charAt(i))) i--;
                    
                    if (i < 0) continue;
                    
                    String rowNumStr = code.substring(0, i + 1);
                    String letters = code.substring(i + 1);

                    if (rowNumStr.isEmpty() || letters.isEmpty()) continue;

                    int rowNum;
                    try {
                        rowNum = Integer.parseInt(rowNumStr);
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    char letter = letters.charAt(0);

                    if (firstRowNumber == -1) {
                        firstRowNumber = rowNum;
                    }

                    if (rowNum == firstRowNumber) {
                        lettersFirstRow.add(letter);
                    } else {
                        break;
                    }
                }
                conn.commit();

                if (!lettersFirstRow.isEmpty()) {
                    logger.info("Asientos por fila inferidos: {} para vuelo: {}", lettersFirstRow.size(), flightId);
                    return lettersFirstRow.size();
                }
            }
        } catch (Exception e) {
            logger.error("Error al inferir asientos por fila para vuelo: {}", flightId, e);
        }
        return null;
    }

}
