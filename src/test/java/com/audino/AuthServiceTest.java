package com.audino;

import com.audino.dao.AccountDAO;
import com.audino.dao.AuditLogDAO;
import com.audino.exception.AccountDisabledException;
import com.audino.exception.AuthenticationException;
import com.audino.exception.ValidationException;
import com.audino.model.Account;
import com.audino.model.AuditLog;
import com.audino.model.Doctor;
import com.audino.model.Role;
import com.audino.service.AuditService;
import com.audino.service.AuthService;
import com.audino.util.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit Test: AuthService & BCrypt Security")
class AuthServiceTest {

    private AuthService authService;
    private MockAccountDAO accountDAO;
    private MockAuditLogDAO auditLogDAO;
    private Doctor activeDoctor;
    private Doctor inactiveDoctor;

    @BeforeEach
    void setUp() {
        accountDAO = new MockAccountDAO();
        auditLogDAO = new MockAuditLogDAO();
        AuditService auditService = new AuditService(auditLogDAO);

        // Use low cost (4) for ultra-fast unit testing
        authService = new AuthService(accountDAO, auditService, 4);
        SessionManager.getInstance().clearSession();

        String rawPassword = "Password@123";
        String hashed = BCrypt.hashpw(rawPassword, BCrypt.gensalt(4));

        activeDoctor = new Doctor(1L, "dr.smith", hashed, "Dr. John Smith", true, "Cardiology", "MCI-12345");
        inactiveDoctor = new Doctor(2L, "dr.inactive", hashed, "Dr. Inactive", false, "General", "MCI-99999");

        accountDAO.saveOrUpdateAccount(activeDoctor);
        accountDAO.saveOrUpdateAccount(inactiveDoctor);
    }

    @Test
    @DisplayName("authenticate() succeeds with valid credentials and sets current session")
    void testSuccessfulAuthentication() throws Exception {
        Account authenticated = authService.authenticate("dr.smith", "Password@123");

        assertThat(authenticated).isNotNull();
        assertThat(authenticated.getUsername()).isEqualTo("dr.smith");
        assertThat(authenticated.getRole()).isEqualTo(Role.DOCTOR);
        assertThat(SessionManager.getInstance().getCurrentAccount()).isSameAs(authenticated);
        assertThat(SessionManager.getInstance().isLoggedIn()).isTrue();

        // Verify successful login audit
        assertThat(auditLogDAO.logs).anyMatch(log ->
                "LOGIN_SUCCESS".equals(log.getOperationType()) && "dr.smith".equals(log.getOperatorUsername())
        );
    }

    @Test
    @DisplayName("authenticate() throws AuthenticationException on empty or null credentials")
    void testEmptyCredentialsThrow() {
        assertThatThrownBy(() -> authService.authenticate("", "Password@123"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("required");

        assertThatThrownBy(() -> authService.authenticate("dr.smith", null))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("authenticate() throws AuthenticationException when username does not exist")
    void testNonExistentUserThrow() {
        assertThatThrownBy(() -> authService.authenticate("unknown_user", "Password@123"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");

        assertThat(auditLogDAO.logs).anyMatch(log ->
                "LOGIN_FAILURE".equals(log.getOperationType()) && "unknown_user".equals(log.getOperatorUsername())
        );
    }

    @Test
    @DisplayName("authenticate() throws AuthenticationException on incorrect password")
    void testIncorrectPasswordThrow() {
        assertThatThrownBy(() -> authService.authenticate("dr.smith", "WrongPassword"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");

        assertThat(SessionManager.getInstance().isLoggedIn()).isFalse();
        assertThat(auditLogDAO.logs).anyMatch(log ->
                "LOGIN_FAILURE".equals(log.getOperationType()) && "dr.smith".equals(log.getOperatorUsername())
        );
    }

    @Test
    @DisplayName("authenticate() throws AccountDisabledException when account is deactivated")
    void testDeactivatedAccountThrow() {
        assertThatThrownBy(() -> authService.authenticate("dr.inactive", "Password@123"))
                .isInstanceOf(AccountDisabledException.class)
                .hasMessageContaining("deactivated");

        assertThat(SessionManager.getInstance().isLoggedIn()).isFalse();
        assertThat(auditLogDAO.logs).anyMatch(log ->
                "LOGIN_FAILURE".equals(log.getOperationType()) && "dr.inactive".equals(log.getOperatorUsername())
        );
    }

    @Test
    @DisplayName("changePassword() validates length >= 8 characters")
    void testPasswordLengthValidation() {
        assertThatThrownBy(() -> authService.changePassword("dr.smith", "Password@123", "short", "short"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    @DisplayName("changePassword() validates new password confirmation match")
    void testPasswordConfirmationMismatch() {
        assertThatThrownBy(() -> authService.changePassword("dr.smith", "Password@123", "NewPassword123", "Mismatch123"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    @DisplayName("changePassword() rejects incorrect current password")
    void testWrongCurrentPasswordRejected() {
        assertThatThrownBy(() -> authService.changePassword("dr.smith", "WrongCurrent", "NewPassword123", "NewPassword123"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("verification failed");
    }

    @Test
    @DisplayName("changePassword() succeeds and updates password hash in DAO")
    void testSuccessfulPasswordChange() throws Exception {
        authService.changePassword("dr.smith", "Password@123", "BrandNewSecurePass!", "BrandNewSecurePass!");

        Account updated = accountDAO.findByUsername("dr.smith").orElseThrow();
        assertThat(BCrypt.checkpw("BrandNewSecurePass!", updated.getPasswordHash())).isTrue();
        assertThat(BCrypt.checkpw("Password@123", updated.getPasswordHash())).isFalse();

        assertThat(auditLogDAO.logs).anyMatch(log ->
                "PASSWORD_CHANGE".equals(log.getOperationType()) && "dr.smith".equals(log.getOperatorUsername())
        );
    }

    // --- Mock In-Memory DAOs for Unit Testing ---

    private static class MockAccountDAO implements AccountDAO {
        private final Map<String, Account> accountsByUsername = new HashMap<>();
        private final Map<Long, Account> accountsById = new HashMap<>();

        void saveOrUpdateAccount(Account account) {
            accountsByUsername.put(account.getUsername().toLowerCase(), account);
            if (account.getId() != null) {
                accountsById.put(account.getId(), account);
            }
        }

        @Override
        public Optional<Account> findById(Long id) {
            return Optional.ofNullable(accountsById.get(id));
        }

        @Override
        public Optional<Account> findByUsername(String username) {
            return Optional.ofNullable(accountsByUsername.get(username.toLowerCase()));
        }

        @Override
        public List<Account> findAll() {
            return new ArrayList<>(accountsByUsername.values());
        }

        @Override
        public Account save(Account account) {
            saveOrUpdateAccount(account);
            return account;
        }

        @Override
        public void update(Account account) {
            saveOrUpdateAccount(account);
        }

        @Override
        public void updatePassword(String username, String newPasswordHash) {
            Account existing = accountsByUsername.get(username.toLowerCase());
            if (existing != null) {
                existing.setPasswordHash(newPasswordHash);
            }
        }

        @Override
        public void setActiveStatus(String username, boolean active) {
            Account existing = accountsByUsername.get(username.toLowerCase());
            if (existing != null) {
                existing.setActive(active);
            }
        }

        @Override
        public int countActiveStaff() {
            return (int) accountsByUsername.values().stream().filter(Account::isActive).count();
        }
    }

    private static class MockAuditLogDAO implements AuditLogDAO {
        final List<AuditLog> logs = new ArrayList<>();

        @Override
        public AuditLog save(AuditLog log) {
            logs.add(log);
            return log;
        }

        @Override
        public List<AuditLog> findRecent(int limit) {
            return new ArrayList<>(logs);
        }

        @Override
        public List<AuditLog> search(String operator, String operationType, LocalDate startDate, LocalDate endDate, int limit) {
            return logs.stream().filter(l ->
                    (operator == null || operator.equalsIgnoreCase(l.getOperatorUsername())) &&
                    (operationType == null || operationType.equalsIgnoreCase(l.getOperationType()))
            ).toList();
        }
    }
}
