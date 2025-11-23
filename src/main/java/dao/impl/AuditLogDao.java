package dao.impl;

import config.db.DBConnection;
import java.sql.*;
import java.util.*;
import model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Access Object para AuditLog.
 * Registra todas las acciones del sistema.
 */
public class AuditLogDao {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogDao.class);

    public boolean insert(AuditLog log) {
        String sql = "INSERT INTO AuditLog (user_id, action, details, created_at) VALUES (?,?,?,SYSUTCDATETIME())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (log.getUserId() > 0) {
                ps.setInt(1, log.getUserId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, log.getAction());
            ps.setString(3, log.getDescription());
            boolean result = ps.executeUpdate() > 0;
            if (result) {
                conn.commit();
            }
            return result;
        } catch (SQLException e) {
            logger.error("Error al insertar auditoría", e);
        }
        return false;
    }

    public List<AuditLog> listAll() {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT audit_id, user_id, action, details, created_at FROM AuditLog ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
            conn.commit();
        } catch (SQLException e) {
            logger.error("Error al listar auditorías", e);
        }
        return list;
    }

    private AuditLog map(ResultSet rs) throws SQLException {
        return new AuditLog(
            rs.getInt("audit_id"),
            rs.getInt("user_id"),
            rs.getString("action"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getString("details")
        );
    }
}
