package com.audino.factory;

import com.audino.model.Account;
import com.audino.model.Admin;
import com.audino.model.Doctor;
import com.audino.model.Receptionist;
import com.audino.model.Role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Factory class encapsulating the polymorphic instantiation of Account subclasses.
 *
 * // OOM: Factory Pattern – encapsulates polymorphic instantiation of Account subclasses based on role.
 */
public class AccountFactory {

    private AccountFactory() {
        // Utility factory
    }

    /**
     * Instantiates the concrete Account subclass corresponding to the specified Role.
     */
    public static Account createAccount(Long id, String username, String passwordHash, String fullName,
                                        Role role, String specialization, String registrationNo,
                                        boolean active, LocalDateTime createdAt) {
        if (role == null) {
            throw new IllegalArgumentException("Account role must not be null");
        }
        switch (role) {
            case ADMIN:
                return new Admin(id, username, passwordHash, fullName, active, createdAt);
            case DOCTOR:
                return new Doctor(id, username, passwordHash, fullName,
                        specialization != null ? specialization : "General Medicine",
                        registrationNo != null ? registrationNo : "PENDING",
                        active, createdAt);
            case RECEPTIONIST:
                return new Receptionist(id, username, passwordHash, fullName, active, createdAt);
            default:
                throw new IllegalArgumentException("Unsupported account role: " + role);
        }
    }

    /**
     * Converts a database ResultSet row into the appropriate Account subclass.
     */
    public static Account fromResultSet(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String fullName = rs.getString("full_name");
        Role role = Role.valueOf(rs.getString("role"));
        String specialization = rs.getString("specialization");
        String registrationNo = rs.getString("registration_no");
        boolean active = rs.getBoolean("active");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        return createAccount(id, username, passwordHash, fullName, role, specialization, registrationNo, active, createdAt);
    }
}
