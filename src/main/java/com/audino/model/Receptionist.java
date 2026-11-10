package com.audino.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Front desk reception staff responsible for patient intake, triage, vitals, and queue token issuance.
 */
public class Receptionist extends Account {

    public Receptionist(Long id, String username, String passwordHash, String fullName, boolean active, LocalDateTime createdAt) {
        super(id, username, passwordHash, fullName, Role.RECEPTIONIST, active, createdAt);
    }

    public Receptionist(Long id, String username, String passwordHash, String fullName, boolean active) {
        this(id, username, passwordHash, fullName, active, LocalDateTime.now());
    }

    public Receptionist(String username, String passwordHash, String fullName) {
        super(null, username, passwordHash, fullName, Role.RECEPTIONIST, true, LocalDateTime.now());
    }

    @Override
    public String getDashboardFxml() {
        return "/fxml/receptionist_dashboard.fxml";
    }

    @Override
    public List<String> getPermissions() {
        return List.of(
            "PATIENT_REGISTER",
            "PATIENT_EDIT",
            "VITALS_RECORD",
            "TOKEN_ISSUE",
            "QUEUE_VIEW"
        );
    }
}
