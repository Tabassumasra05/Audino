package com.audino.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Consulting medical physician registered with the medical council.
 * Possesses prescribing authority, consultation queue access, and override capabilities.
 */
public class Doctor extends Account {

    private String specialization;
    private String registrationNumber;

    public Doctor(Long id, String username, String passwordHash, String fullName,
                  String specialization, String registrationNumber,
                  boolean active, LocalDateTime createdAt) {
        super(id, username, passwordHash, fullName, Role.DOCTOR, active, createdAt);
        this.specialization = Objects.requireNonNull(specialization, "Specialization must not be null");
        this.registrationNumber = Objects.requireNonNull(registrationNumber, "Registration number must not be null");
    }

    public Doctor(Long id, String username, String passwordHash, String fullName,
                  boolean active, String specialization, String registrationNumber) {
        this(id, username, passwordHash, fullName, specialization, registrationNumber, active, LocalDateTime.now());
    }

    public Doctor(String username, String passwordHash, String fullName,
                  String specialization, String registrationNumber) {
        super(null, username, passwordHash, fullName, Role.DOCTOR, true, LocalDateTime.now());
        this.specialization = specialization;
        this.registrationNumber = registrationNumber;
    }

    @Override
    public String getDashboardFxml() {
        return "/fxml/doctor_dashboard.fxml";
    }

    @Override
    public List<String> getPermissions() {
        return List.of(
            "PATIENT_CONSULT",
            "PRESCRIPTION_CREATE",
            "PRESCRIPTION_OVERRIDE",
            "VITALS_VIEW",
            "QUEUE_MANAGE"
        );
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = Objects.requireNonNull(specialization);
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = Objects.requireNonNull(registrationNumber);
    }
}
