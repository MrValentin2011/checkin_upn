/*
 * DAO para preguntas frecuentes (FAQ)
 */
package dao.impl;

import config.db.DBConnection;
import model.FAQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Acceso a datos para preguntas frecuentes.
 * Gestiona búsqueda, categorización y estadísticas de FAQ.
 */
public class FAQDao {
    private static final Logger logger = LoggerFactory.getLogger(FAQDao.class);

    /**
     * Crea la tabla de FAQ si no existe
     */
    public static void createTableIfNotExists() {
        String sqlCheckTable = """
                IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'FAQ' AND type = 'U')
                CREATE TABLE FAQ (
                    faq_id INT IDENTITY(1,1) PRIMARY KEY,
                    category NVARCHAR(100) NOT NULL,
                    question NVARCHAR(MAX) NOT NULL,
                    answer NVARCHAR(MAX) NOT NULL,
                    keywords NVARCHAR(MAX),
                    helpful_yes INT DEFAULT 0,
                    helpful_no INT DEFAULT 0,
                    views INT DEFAULT 0,
                    created_at DATETIME DEFAULT GETDATE(),
                    updated_at DATETIME NULL,
                    active BIT DEFAULT 1
                );
                """;

        String idxCategory = """
                IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_category')
                CREATE INDEX idx_category ON FAQ(category);
                """;

        String idxActive = """
                IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_active')
                CREATE INDEX idx_active ON FAQ(active);
                """;

        String idxQuestion = """
                IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_question_search')
                CREATE INDEX idx_question_search ON FAQ(question);
                """;

        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.execute(sqlCheckTable);
            stmt.execute(idxCategory);
            stmt.execute(idxActive);
            stmt.execute(idxQuestion);

            logger.info("Tabla FAQ e índices verificados/creados");

        } catch (SQLException e) {
            logger.error("Error creando tabla de FAQ", e);
        }
    }

    /**
     * Inserta una nueva FAQ
     */
    public boolean insert(FAQ faq) {
        String sql = """
                INSERT INTO FAQ (category, question, answer, keywords, active)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, faq.getCategory());
            ps.setString(2, faq.getQuestion());
            ps.setString(3, faq.getAnswer());
            ps.setString(4, faq.getKeywords());
            ps.setBoolean(5, faq.isActive());

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        faq.setFaqId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error insertando FAQ", e);
        }
        return false;
    }

    /**
     * Obtiene una FAQ por ID
     */
    public FAQ findById(int faqId) {
        String sql = "SELECT * FROM FAQ WHERE faq_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, faqId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Incrementar contador de vistas
                    incrementViews(faqId);
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo FAQ", e);
        }
        return null;
    }

    /**
     * Obtiene FAQs por categoría
     */
    public List<FAQ> getByCategory(String category) {
        String sql = "SELECT * FROM FAQ WHERE category = ? AND active = 1 ORDER BY helpful_yes DESC, views DESC";

        List<FAQ> faqs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    faqs.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo FAQs por categoría", e);
        }
        return faqs;
    }

    /**
     * Búsqueda de FAQs por texto
     */
    public List<FAQ> search(String query) {
        String sql = """
                SELECT * FROM FAQ
                WHERE active = 1 AND (
                    question LIKE ? OR
                    answer LIKE ? OR
                    keywords LIKE ?
                )
                ORDER BY helpful_yes DESC, views DESC
                """;

        List<FAQ> results = new ArrayList<>();
        String searchPattern = "%" + query + "%";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error buscando FAQs", e);
        }
        return results;
    }

    /**
     * Obtiene todas las FAQs activas
     */
    public List<FAQ> getAll() {
        String sql = "SELECT * FROM FAQ WHERE active = 1 ORDER BY category, helpful_yes DESC";

        List<FAQ> faqs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                faqs.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo todas las FAQs", e);
        }
        return faqs;
    }

    /**
     * Obtiene FAQs más vistas
     */
    public List<FAQ> getTopViewed(int limit) {
        String sql = "SELECT * FROM FAQ WHERE active = 1 ORDER BY views DESC LIMIT ?";

        List<FAQ> faqs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    faqs.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo FAQs más vistas", e);
        }
        return faqs;
    }

    /**
     * Obtiene FAQs más útiles
     */
    public List<FAQ> getMostHelpful(int limit) {
        String sql = """
                SELECT * FROM FAQ
                WHERE active = 1 AND (helpful_yes + helpful_no) > 0
                ORDER BY (CAST(helpful_yes AS FLOAT) / (helpful_yes + helpful_no)) DESC
                LIMIT ?
                """;

        List<FAQ> faqs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    faqs.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo FAQs útiles", e);
        }
        return faqs;
    }

    /**
     * Obtiene las categorías disponibles
     */
    public Set<String> getCategories() {
        Set<String> categories = new LinkedHashSet<>();
        String sql = "SELECT DISTINCT category FROM FAQ WHERE active = 1 ORDER BY category";

        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo categorías", e);
        }
        return categories;
    }

    /**
     * Marca una FAQ como útil
     */
    public void markHelpful(int faqId) {
        String sql = "UPDATE FAQ SET helpful_yes = helpful_yes + 1 WHERE faq_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, faqId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error marcando FAQ como útil", e);
        }
    }

    /**
     * Marca una FAQ como no útil
     */
    public void markNotHelpful(int faqId) {
        String sql = "UPDATE FAQ SET helpful_no = helpful_no + 1 WHERE faq_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, faqId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error marcando FAQ como no útil", e);
        }
    }

    /**
     * Incrementa contador de vistas
     */
    public void incrementViews(int faqId) {
        String sql = "UPDATE FAQ SET views = views + 1 WHERE faq_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, faqId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error incrementando vistas", e);
        }
    }

    /**
     * Actualiza una FAQ
     */
    public boolean update(FAQ faq) {
        String sql = """
                UPDATE FAQ
                SET category = ?, question = ?, answer = ?, keywords = ?, active = ?, updated_at = CURRENT_TIMESTAMP
                WHERE faq_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, faq.getCategory());
            ps.setString(2, faq.getQuestion());
            ps.setString(3, faq.getAnswer());
            ps.setString(4, faq.getKeywords());
            ps.setBoolean(5, faq.isActive());
            ps.setInt(6, faq.getFaqId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error actualizando FAQ", e);
        }
        return false;
    }

    /**
     * Mapea ResultSet a FAQ
     */
    private FAQ mapResultSet(ResultSet rs) throws SQLException {
        FAQ faq = new FAQ();
        faq.setFaqId(rs.getInt("faq_id"));
        faq.setCategory(rs.getString("category"));
        faq.setQuestion(rs.getString("question"));
        faq.setAnswer(rs.getString("answer"));
        faq.setKeywords(rs.getString("keywords"));
        faq.setHelpfulYes(rs.getInt("helpful_yes"));
        faq.setHelpfulNo(rs.getInt("helpful_no"));
        faq.setViews(rs.getInt("views"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            faq.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            faq.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        faq.setActive(rs.getBoolean("active"));

        return faq;
    }
}
