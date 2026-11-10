package com.audino.dao.jdbc;

import com.audino.dao.AuditLogDAO;
import com.audino.exception.DataAccessException;
import com.audino.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JDBC implementation of AuditLogDAO for persistent forensic security trails.
 */
public class AuditLogDAOJdbc implements AuditLogDAO {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogDAOJdbc.class);
    private final DataSource dataSource;

    public AuditLogDAOJdbc(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public AuditLog save(AuditLog log) {
        String sql = "INSERT INTO audit_logs (timestamp, operator_username, operation_type, entity_type, entity_id, details) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(log.getTimestamp()));
            ps.setString(2, log.getOperatorUsername());
            ps.setString(3, log.getOperationType());
            ps.setString(4, log.getEntityType());
            ps.setString(5, log.getEntityId());
            ps.setString(6, log.getDetails());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    log.setId(keys.getLong(1));
                }
            }
            return log;
        } catch (SQLException e) {
            logger.error("Error saving audit log: {}", e.getMessage());
            throw new DataAccessException("Failed to record audit log", e);
        }
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching recent audit logs: {}", e.getMessage());
            throw new DataAccessException("Failed to fetch audit logs", e);
        }
        return list;
    }

    @Override
    public List<AuditLog> search(String operator, String operationType, LocalDate startDate, LocalDate endDate, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_logs WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (operator != null && !operator.isBlank()) {
            sql.append("AND LOWER(operator_username) = LOWER(?) ");
            params.add(operator.trim());
        }
        if (operationType != null && !operationType.isBlank()) {
            sql.append("AND operation_type = ? ");
            params.add(operationType.trim());
        }
        if (startDate != null) {
            sql.append("AND DATE(timestamp) >= ? ");
            params.add(Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append("AND DATE(timestamp) <= ? ");
            params.add(Date.valueOf(endDate));
        }

        sql.append("ORDER BY timestamp DESC LIMIT ?");
        params.add(limit > 0 ? limit : 100);

        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching audit logs: {}", e.getMessage());
            throw new DataAccessException("Failed to search audit logs", e);
        }
        return list;
    }

    private AuditLog mapAuditLog(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Timestamp ts = rs.getTimestamp("timestamp");
        LocalDateTime timestamp = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        String op = rs.getString("operator_username");
        String action = rs.getString("operation_type");
        String entity = rs.getString("entity_type");
        String entityId = rs.getString("entity_id");
        String details = rs.getString("details");

        return new AuditLog(id, timestamp, op, action, entity, entityId, details);
    }
}
