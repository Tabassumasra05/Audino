package com.audino.dao.jdbc;

import com.audino.dao.TokenDAO;
import com.audino.exception.DataAccessException;
import com.audino.model.Token;
import com.audino.model.TokenStatus;
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
import java.util.Optional;

/**
 * JDBC implementation of TokenDAO managing outpatient queue state.
 */
public class TokenDAOJdbc implements TokenDAO {

    private static final Logger logger = LoggerFactory.getLogger(TokenDAOJdbc.class);
    private final DataSource dataSource;

    public TokenDAOJdbc(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Optional<Token> findById(Long id) {
        String sql = "SELECT * FROM tokens WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapToken(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding token ID {}: {}", id, e.getMessage());
            throw new DataAccessException("Failed to find token", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Token> findByTokenNoAndDate(String tokenNo, LocalDate date) {
        String sql = "SELECT * FROM tokens WHERE UPPER(token_no) = UPPER(?) AND token_date = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenNo.trim());
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapToken(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding token {} for date {}: {}", tokenNo, date, e.getMessage());
            throw new DataAccessException("Failed to find token by number and date", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Token> findTodayTokens() {
        String sql = "SELECT * FROM tokens WHERE token_date = CURRENT_DATE ORDER BY id ASC";
        List<Token> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapToken(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding today's tokens: {}", e.getMessage());
            throw new DataAccessException("Failed to fetch today's tokens", e);
        }
        return list;
    }

    @Override
    public List<Token> findTodayTokensByDoctor(String doctorUsername) {
        String sql = "SELECT * FROM tokens WHERE token_date = CURRENT_DATE AND (doctor_username = ? OR doctor_username IS NULL) ORDER BY id ASC";
        List<Token> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctorUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapToken(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding tokens for doctor {}: {}", doctorUsername, e.getMessage());
            throw new DataAccessException("Failed to fetch doctor's tokens", e);
        }
        return list;
    }

    @Override
    public Optional<Token> findNextWaitingToken(String doctorUsername, LocalDate date) {
        String sql = "SELECT * FROM tokens WHERE token_date = ? AND status = 'WAITING' AND (doctor_username = ? OR doctor_username IS NULL) ORDER BY id ASC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, doctorUsername);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapToken(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding next waiting token for {}: {}", doctorUsername, e.getMessage());
            throw new DataAccessException("Failed to fetch next waiting token", e);
        }
        return Optional.empty();
    }

    @Override
    public Token save(Token token) {
        String sql = "INSERT INTO tokens (token_no, token_date, patient_id, doctor_username, status, issued_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, token.getTokenNo());
            ps.setDate(2, Date.valueOf(token.getTokenDate()));
            ps.setLong(3, token.getPatientId());
            ps.setString(4, token.getDoctorUsername());
            ps.setString(5, token.getStatus().name());
            ps.setTimestamp(6, Timestamp.valueOf(token.getIssuedAt()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    token.setId(keys.getLong(1));
                }
            }
            return token;
        } catch (SQLException e) {
            logger.error("Error saving token {}: {}", token.getTokenNo(), e.getMessage());
            throw new DataAccessException("Failed to persist token", e);
        }
    }

    @Override
    public void updateStatus(Long tokenId, TokenStatus newStatus) {
        String sql = "UPDATE tokens SET status = ?, " +
                     "called_at = CASE WHEN ? = 'IN_CONSULTATION' AND called_at IS NULL THEN CURRENT_TIMESTAMP ELSE called_at END, " +
                     "completed_at = CASE WHEN ? = 'COMPLETED' AND completed_at IS NULL THEN CURRENT_TIMESTAMP ELSE completed_at END " +
                     "WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, newStatus.name());
            ps.setString(3, newStatus.name());
            ps.setLong(4, tokenId);

            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating token {} status to {}: {}", tokenId, newStatus, e.getMessage());
            throw new DataAccessException("Failed to update token status", e);
        }
    }

    @Override
    public void assignDoctor(Long tokenId, String doctorUsername) {
        String sql = "UPDATE tokens SET doctor_username = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctorUsername);
            ps.setLong(2, tokenId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error assigning doctor {} to token {}: {}", doctorUsername, tokenId, e.getMessage());
            throw new DataAccessException("Failed to assign doctor", e);
        }
    }

    @Override
    public String getNextTokenNo(LocalDate date) {
        String sql = "SELECT MAX(token_no) FROM tokens WHERE token_date = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String max = rs.getString(1);
                    if (max != null && max.startsWith("T-")) {
                        try {
                            int num = Integer.parseInt(max.substring(2));
                            return String.format("T-%03d", num + 1);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error generating next token number: {}", e.getMessage());
            throw new DataAccessException("Failed to generate token number", e);
        }
        return "T-001";
    }

    @Override
    public int countTodayTokensByStatus(TokenStatus status) {
        String sql = "SELECT COUNT(*) FROM tokens WHERE token_date = CURRENT_DATE AND status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting today tokens by status {}: {}", status, e.getMessage());
            throw new DataAccessException("Failed to count tokens", e);
        }
        return 0;
    }

    private Token mapToken(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String tokenNo = rs.getString("token_no");
        LocalDate tokenDate = rs.getDate("token_date").toLocalDate();
        Long patientId = rs.getLong("patient_id");
        String doc = rs.getString("doctor_username");
        TokenStatus status = TokenStatus.valueOf(rs.getString("status"));
        Timestamp ts1 = rs.getTimestamp("issued_at");
        Timestamp ts2 = rs.getTimestamp("called_at");
        Timestamp ts3 = rs.getTimestamp("completed_at");

        LocalDateTime issuedAt = ts1 != null ? ts1.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime calledAt = ts2 != null ? ts2.toLocalDateTime() : null;
        LocalDateTime completedAt = ts3 != null ? ts3.toLocalDateTime() : null;

        return new Token(id, tokenNo, tokenDate, patientId, doc, status, issuedAt, calledAt, completedAt);
    }
}
