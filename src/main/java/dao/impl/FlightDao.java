package dao.impl;

import config.db.DBConnection;
import java.sql.*;
import java.util.*;
import model.Flight;

public class FlightDao {

    /**
     * Lista todos los vuelos con JOIN hacia Airlines y Destinations.
     */
    public List<Flight> listAll(String filtro) {
        List<Flight> flights = new ArrayList<>();

        String baseSQL = """
            SELECT 
                f.flight_id,
                f.flight_number,
                a.name AS airline_name,
                o.city AS origin_city,
                d.city AS destination_city,
                f.departure_time,
                f.arrival_time,
                f.status,
                f.capacity
            FROM Flights f
            INNER JOIN Airlines a ON f.airline_id = a.airline_id
            INNER JOIN Destinations o ON f.origin_id = o.destination_id
            INNER JOIN Destinations d ON f.destination_id = d.destination_id
            WHERE 1=1
            """;

        // filtro opcional
        if (filtro != null && !filtro.isBlank()) {
            baseSQL += """
                AND (
                    f.flight_number LIKE ? OR
                    a.name LIKE ? OR
                    o.city LIKE ? OR
                    d.city LIKE ?
                )
                """;
        }

        baseSQL += " ORDER BY f.departure_time DESC";

        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(baseSQL)) {

            if (filtro != null && !filtro.isBlank()) {
                String f = "%" + filtro + "%";
                ps.setString(1, f);
                ps.setString(2, f);
                ps.setString(3, f);
                ps.setString(4, f);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Flight flight = new Flight();
                flight.setId(rs.getInt("flight_id"));
                flight.setCode(rs.getString("flight_number"));
                flight.setAirline(rs.getString("airline_name"));
                flight.setOrigin(rs.getString("origin_city"));
                flight.setDestination(rs.getString("destination_city"));
                flight.setDepartureTime(rs.getTimestamp("departure_time").toLocalDateTime());
                Timestamp arr = rs.getTimestamp("arrival_time");
                if (arr != null) {
                    flight.setArrivalTime(arr.toLocalDateTime());
                }
                flight.setStatus(rs.getString("status"));
                flight.setCapacity(rs.getInt("capacity"));
                flights.add(flight);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flights;
    }

    /**
     * Inserta un nuevo vuelo.
     */
    public boolean insert(Flight flight, int airlineId, int originId, int destinationId) {
        String sql = """
            INSERT INTO Flights 
            (flight_number, airline_id, origin_id, destination_id, departure_time, arrival_time, status, capacity)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);

            ps.setString(1, flight.getCode());
            ps.setInt(2, airlineId);
            ps.setInt(3, originId);
            ps.setInt(4, destinationId);
            ps.setTimestamp(5, Timestamp.valueOf(flight.getDepartureTime()));
            ps.setTimestamp(6, flight.getArrivalTime() != null ? Timestamp.valueOf(flight.getArrivalTime()) : null);
            ps.setString(7, flight.getStatus());
            ps.setInt(8, flight.getCapacity());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                conn.commit();
                conn.setAutoCommit(true);
                return true;
            } else {
                conn.rollback();
                conn.setAutoCommit(true);
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // dao.impl.FlightDao (añade este nuevo método)
public boolean insertWithSeats(Flight flight, int airlineId, int originId, int destinationId,
                               LinkedHashMap<String,Integer> classDistribution, int seatsPerRow) {
    String sql = """
        INSERT INTO Flights 
        (flight_number, airline_id, origin_id, destination_id, departure_time, arrival_time, status, capacity)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """;
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        conn.setAutoCommit(false);

        ps.setString(1, flight.getCode());
        ps.setInt(2, airlineId);
        ps.setInt(3, originId);
        ps.setInt(4, destinationId);
        ps.setTimestamp(5, java.sql.Timestamp.valueOf(flight.getDepartureTime()));
        ps.setTimestamp(6, (flight.getArrivalTime() != null) ? java.sql.Timestamp.valueOf(flight.getArrivalTime()) : null);
        ps.setString(7, flight.getStatus());
        ps.setInt(8, flight.getCapacity());

        int affected = ps.executeUpdate();
        if (affected == 0) { conn.rollback(); conn.setAutoCommit(true); return false; }

        int flightId;
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) { conn.rollback(); conn.setAutoCommit(true); return false; }
            flightId = keys.getInt(1);
        }

        // Validación simple: la suma de la distribución debe igualar capacidad
        int sum = classDistribution.values().stream().mapToInt(v -> v == null ? 0 : v).sum();
        if (sum != flight.getCapacity()) {
            conn.rollback(); conn.setAutoCommit(true);
            throw new IllegalArgumentException("La suma de asientos por clase (" + sum + ") no coincide con la capacidad (" + flight.getCapacity() + ")");
        }

        // Crear asientos
        SeatDao seatDao = new SeatDao();
        boolean seatsOk = seatDao.bulkCreateSeatsByClass(conn, flightId, classDistribution, seatsPerRow);
        if (!seatsOk) { conn.rollback(); conn.setAutoCommit(true); return false; }

        conn.commit();
        conn.setAutoCommit(true);
        return true;

    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}


    public boolean update(Flight flight, int airlineId, int originId, int destinationId) {
        String sql = """
        UPDATE Flights SET
            flight_number = ?,
            airline_id = ?,
            origin_id = ?,
            destination_id = ?,
            departure_time = ?,
            arrival_time = ?,
            status = ?,
            capacity = ?
        WHERE flight_id = ?
        """;
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);

            ps.setString(1, flight.getCode());
            ps.setInt(2, airlineId);
            ps.setInt(3, originId);
            ps.setInt(4, destinationId);
            ps.setTimestamp(5, Timestamp.valueOf(flight.getDepartureTime()));
            ps.setTimestamp(6, flight.getArrivalTime() != null ? Timestamp.valueOf(flight.getArrivalTime()) : null);
            ps.setString(7, flight.getStatus());
            ps.setInt(8, flight.getCapacity());
            ps.setInt(9, flight.getId());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                conn.commit();
                conn.setAutoCommit(true);
                return true;
            } else {
                conn.rollback();
                conn.setAutoCommit(true);
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean delete(int flightId) {
    String sql = "DELETE FROM Flights WHERE flight_id = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        conn.setAutoCommit(false);
        ps.setInt(1, flightId);
        int affected = ps.executeUpdate();
        if (affected > 0) {
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } else {
            conn.rollback();
            conn.setAutoCommit(true);
            return false;
        }
    } catch (SQLException e) {
        // Si hay una restricción FK (reservas, check-in, etc.), SQL lanzará excepción
        System.err.println("⚠️ No se pudo eliminar vuelo ID " + flightId + ": " + e.getMessage());
        return false;
    }
}

    /** Obtiene un vuelo por su ID con información de aerolínea y ciudades */
    public Flight findById(int flightId) {
        String sql = "SELECT f.flight_id, f.flight_number, a.name AS airline_name, o.city AS origin_city, d.city AS destination_city, f.departure_time, f.arrival_time, f.status, f.capacity FROM Flights f INNER JOIN Airlines a ON f.airline_id = a.airline_id INNER JOIN Destinations o ON f.origin_id = o.destination_id INNER JOIN Destinations d ON f.destination_id = d.destination_id WHERE f.flight_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, flightId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Flight flight = new Flight();
                    flight.setId(rs.getInt("flight_id"));
                    flight.setCode(rs.getString("flight_number"));
                    flight.setAirline(rs.getString("airline_name"));
                    flight.setOrigin(rs.getString("origin_city"));
                    flight.setDestination(rs.getString("destination_city"));
                    Timestamp dep = rs.getTimestamp("departure_time");
                    if (dep != null) flight.setDepartureTime(dep.toLocalDateTime());
                    Timestamp arr = rs.getTimestamp("arrival_time");
                    if (arr != null) flight.setArrivalTime(arr.toLocalDateTime());
                    flight.setStatus(rs.getString("status"));
                    flight.setCapacity(rs.getInt("capacity"));
                    return flight;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
