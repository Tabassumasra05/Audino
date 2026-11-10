package com.audino.service;

import com.audino.dao.AccountDAO;
import com.audino.exception.AccountDisabledException;
import com.audino.exception.AuthenticationException;
import com.audino.exception.ValidationException;
import com.audino.model.Account;
import com.audino.util.SessionManager;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Service managing user authentication, cryptographic password verification,
 * and credential updates.
 *
 * // OOM: Single Responsibility Principle (SRP) – solely responsible for authentication and credentials security.
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AccountDAO accountDAO;
    private final AuditService auditService;
    private final int bcryptCost;

    public AuthService(AccountDAO accountDAO, AuditService auditService, int bcryptCost) {
        this.accountDAO = Objects.requireNonNull(accountDAO, "AccountDAO must not be null");
        this.auditService = Objects.requireNonNull(auditService, "AuditService must not be null");
        this.bcryptCost = bcryptCost > 4 ? bcryptCost : 12;
    }

    public AuthService(AccountDAO accountDAO, AuditService auditService) {
        this(accountDAO, auditService, 12);
    }

    /**
     * Authenticates credentials using adaptive salted BCrypt hashing.
     *
     * @param username Account username
     * @param password Cleartext password input
     * @return Authenticated Account entity
     * @throws AuthenticationException if username not found or password incorrect
     * @throws AccountDisabledException if account active flag is false
     */
    public Account authenticate(String username, String password) throws AuthenticationException, AccountDisabledException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            auditService.logLogin(username, false, "Empty credentials provided");
            throw new AuthenticationException("Username and password are required");
        }

        Optional<Account> accountOpt = accountDAO.findByUsername(username.trim());
        if (accountOpt.isEmpty()) {
            auditService.logLogin(username, false, "Username not found");
            logger.warn("Authentication failed: user '{}' does not exist.", username);
            throw new AuthenticationException("Invalid username or password");
        }

        Account account = accountOpt.get();

        // Verify cryptographic hash
        boolean passwordMatches = false;
        try {
            passwordMatches = BCrypt.checkpw(password, account.getPasswordHash());
        } catch (Exception e) {
            logger.error("Error verifying password hash for {}: {}", username, e.getMessage());
        }

        if (!passwordMatches) {
            auditService.logLogin(username, false, "Invalid password hash match");
            logger.warn("Authentication failed: incorrect password for user '{}'.", username);
            throw new AuthenticationException("Invalid username or password");
        }

        // Verify account is active
        if (!account.isActive()) {
            auditService.logLogin(username, false, "Deactivated account refused");
            logger.warn("Authentication rejected: account '{}' is deactivated.", username);
            throw new AccountDisabledException("This account has been deactivated. Please contact your clinic administrator.");
        }

        // Establish session
        SessionManager.getInstance().setCurrentAccount(account);
        auditService.logLogin(username, true, "Local Terminal");
        logger.info("User '{}' ({}) authenticated successfully.", account.getUsername(), account.getRole());

        return account;
    }

    /**
     * Updates an account password after verifying the existing password.
     */
    public void changePassword(String username, String currentPassword, String newPassword, String confirmPassword)
            throws AuthenticationException, ValidationException {
        if (newPassword == null || newPassword.length() < 8) {
            throw new ValidationException("New password must be at least 8 characters in length");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("New password and confirmation password do not match");
        }

        Optional<Account> accountOpt = accountDAO.findByUsername(username);
        if (accountOpt.isEmpty()) {
            throw new AuthenticationException("Account not found");
        }

        Account account = accountOpt.get();
        if (!BCrypt.checkpw(currentPassword, account.getPasswordHash())) {
            throw new AuthenticationException("Current password verification failed");
        }

        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(bcryptCost));
        accountDAO.updatePassword(username, newHash);
        auditService.logStaffAction(username, username, "PASSWORD_CHANGE", "User successfully changed their password");
        logger.info("Password successfully changed for user '{}'.", username);
    }
}
