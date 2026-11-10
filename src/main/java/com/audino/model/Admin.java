package com.audino.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * System administrator account with governance and maintenance permissions.
 */
public class Admin extends Account {

    public Admin(Long id, String username, String passwordHash, String fullName, boolean active, LocalDateTime createdAt) {
        super(id, username, passwordHash, fullName, Role.ADMIN, active, createdAt);
    }

    public Admin(String username, String passwordHash, String fullName) {
        super(null, username, passwordHash, fullName, Role.ADMIN, true, LocalDateTime.now());
    }

    @Override
    public String getDashboardFxml() {
        return "/fxml/admin_dashboard.fxml";
    }

    @Override
    public List<String> getPermissions() {
        return List.of(
            "STAFF_MANAGE",
            "FORMULARY_MANAGE",
            "RULES_MANAGE",
            "AUDIT_VIEW",
            "KPI_VIEW"
        );
    }
}
