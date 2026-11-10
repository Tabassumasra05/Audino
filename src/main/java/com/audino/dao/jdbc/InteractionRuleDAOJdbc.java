package com.audino.dao.jdbc;

import com.audino.dao.InteractionRuleDAO;
import com.audino.exception.DataAccessException;
import com.audino.model.AlertLevel;
import com.audino.model.AlertType;
import com.audino.model.InteractionRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC implementation of InteractionRuleDAO.
 */
public class InteractionRuleDAOJdbc implements InteractionRuleDAO {

    private static final Logger logger = LoggerFactory.getLogger(InteractionRuleDAOJdbc.class);
    private final DataSource dataSource;

    public InteractionRuleDAOJdbc(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Optional<InteractionRule> findById(Long id) {
        String sql = "SELECT * FROM interaction_rules WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRule(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding interaction rule {}: {}", id, e.getMessage());
            throw new DataAccessException("Failed to find interaction rule", e);
        }
        return Optional.empty();
    }

    @Override
    public List<InteractionRule> findAll() {
        String sql = "SELECT * FROM interaction_rules ORDER BY severity, type, keyword1";
        List<InteractionRule> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRule(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all interaction rules: {}", e.getMessage());
            throw new DataAccessException("Failed to fetch interaction rules", e);
        }
        return list;
    }

    @Override
    public List<InteractionRule> findByType(AlertType type) {
        String sql = "SELECT * FROM interaction_rules WHERE type = ? ORDER BY severity, keyword1";
        List<InteractionRule> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRule(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding rules by type {}: {}", type, e.getMessage());
            throw new DataAccessException("Failed to fetch rules by type", e);
        }
        return list;
    }

    @Override
    public InteractionRule save(InteractionRule rule) {
        String sql = "INSERT INTO interaction_rules (type, keyword1, keyword2, severity, description, recommendation) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, rule.getType().name());
            ps.setString(2, rule.getKeyword1().trim());
            ps.setString(3, rule.getKeyword2().trim());
            ps.setString(4, rule.getSeverity().name());
            ps.setString(5, rule.getDescription().trim());
            ps.setString(6, rule.getRecommendation());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    rule.setId(keys.getLong(1));
                }
            }
            return rule;
        } catch (SQLException e) {
            logger.error("Error saving interaction rule: {}", e.getMessage());
            throw new DataAccessException("Failed to persist interaction rule", e);
        }
    }

    @Override
    public void update(InteractionRule rule) {
        String sql = "UPDATE interaction_rules SET type = ?, keyword1 = ?, keyword2 = ?, severity = ?, description = ?, recommendation = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rule.getType().name());
            ps.setString(2, rule.getKeyword1().trim());
            ps.setString(3, rule.getKeyword2().trim());
            ps.setString(4, rule.getSeverity().name());
            ps.setString(5, rule.getDescription().trim());
            ps.setString(6, rule.getRecommendation());
            ps.setLong(7, rule.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating interaction rule {}: {}", rule.getId(), e.getMessage());
            throw new DataAccessException("Failed to update interaction rule", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM interaction_rules WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting interaction rule {}: {}", id, e.getMessage());
            throw new DataAccessException("Failed to delete interaction rule", e);
        }
    }

    private InteractionRule mapRule(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        AlertType type = AlertType.valueOf(rs.getString("type"));
        String k1 = rs.getString("keyword1");
        String k2 = rs.getString("keyword2");
        AlertLevel severity = AlertLevel.valueOf(rs.getString("severity"));
        String desc = rs.getString("description");
        String rec = rs.getString("recommendation");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        return new InteractionRule(id, type, k1, k2, severity, desc, rec, createdAt);
    }
}
