package com.audino.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class modeling clinical and administrative staff user accounts.
 *
 * // OOM: Inheritance & Abstraction – defines the common contract and encapsulated state for all clinic users.
 */
public abstract class Account {

    private Long id;
    private final String username;
    private String passwordHash;
    private String fullName;
    private final Role role;
    private boolean active;
    private final LocalDateTime createdAt;

    // OOM: Encapsulation – constructor enforces non-null required attributes
    protected Account(Long id, String username, String passwordHash, String fullName, Role role, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "Username must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash must not be null");
        this.fullName = Objects.requireNonNull(fullName, "Full name must not be null");
        this.role = Objects.requireNonNull(role, "Role must not be null");
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * Polymorphic method determining the role-specific JavaFX dashboard view path.
     * Eliminates conditional branching (if role == ...) in login routing.
     *
     * // OOM: Polymorphism – subclass returns its specific dashboard FXML resource path.
     *
     * @return Path to the role's dashboard FXML file.
     */
    public abstract String getDashboardFxml();

    /**
     * Retrieves the permission tokens granted to this account role.
     *
     * @return Immutable list of granted permission strings.
     */
    public abstract List<String> getPermissions();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = Objects.requireNonNull(fullName);
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return Objects.equals(username, account.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
