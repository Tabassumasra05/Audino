package com.audino.dao.jdbc;

import com.audino.dao.AccountDAO;
import com.audino.exception.DataAccessException;
import com.audino.factory.AccountFactory;
import com.audino.model.Account;
import com.audino.model.Doctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC implementation of AccountDAO using prepared statements and connection pooling.
 *
 * // OOM: DAO Pattern – separates relational SQL persistence from domain logic.
 */
public class AccountDAOJdbc implements AccountDAO {

    private static final Logger logger = LoggerFactory.getLogger(AccountDAOJdbc.class);
    private final DataSource dataSource;

    public AccountDAOJdbc(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Optional<Account> findById(Long id) {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(AccountFactory.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding account by ID {}: {}", id, e.getMessage());
            throw new DataAccessException("Failed to find account by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        String sql = "SELECT * FROM accounts WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(AccountFactory.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding account by username {}: {}", username, e.getMessage());
            throw new DataAccessException("Failed to find account by username", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Account> findAll() {
        String sql = "SELECT * FROM accounts ORDER BY role, full_name";
        List<Account> accounts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                accounts.add(AccountFactory.fromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving accounts: {}", e.getMessage());
            throw new DataAccessException("Failed to fetch all accounts", e);
        }
        return accounts;
    }

    @Override
    public Account save(Account account) {
        String sql = "INSERT INTO accounts (username, password_hash, full_name, role, specialization, registration_no, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, account.getUsername().trim());
            ps.setString(2, account.getPasswordHash());
            ps.setString(3, account.getFullName().trim());
            ps.setString(4, account.getRole().name());

            if (account instanceof Doctor doc) {
                ps.setString(5, doc.getSpecialization());
                ps.setString(6, doc.getRegistrationNumber());
            } else {
                ps.setString(5, null);
                ps.setString(6, null);
            }
            ps.setBoolean(7, account.isActive());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        account.setId(keys.getLong(1));
                    }
                }
            }
            return account;
        } catch (SQLException e) {
            logger.error("Error inserting account {}: {}", account.getUsername(), e.getMessage());
            throw new DataAccessException("Failed to persist account", e);
        }
    }

    @Override
    public void update(Account account) {
        String sql = "UPDATE accounts SET full_name = ?, specialization = ?, registration_no = ?, active = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getFullName().trim());
            if (account instanceof Doctor doc) {
                ps.setString(2, doc.getSpecialization());
                ps.setString(3, doc.getRegistrationNumber());
            } else {
                ps.setString(2, null);
                ps.setString(3, null);
            }
            ps.setBoolean(4, account.isActive());
            ps.setLong(5, account.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating account ID {}: {}", account.getId(), e.getMessage());
            throw new DataAccessException("Failed to update account", e);
        }
    }

    @Override
    public void updatePassword(String username, String newPasswordHash) {
        String sql = "UPDATE accounts SET password_hash = ? WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setString(2, username.trim());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating password for {}: {}", username, e.getMessage());
            throw new DataAccessException("Failed to update password", e);
        }
    }

    @Override
    public void setActiveStatus(String username, boolean active) {
        String sql = "UPDATE accounts SET active = ? WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setString(2, username.trim());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error setting active status for {}: {}", username, e.getMessage());
            throw new DataAccessException("Failed to update active status", e);
        }
    }

    @Override
    public int countActiveStaff() {
        String sql = "SELECT COUNT(*) FROM accounts WHERE active = TRUE";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting active staff: {}", e.getMessage());
            throw new DataAccessException("Failed to count active staff", e);
        }
        return 0;
    }
}
