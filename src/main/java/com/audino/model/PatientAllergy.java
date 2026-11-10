package com.audino.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Confirmed patient allergy record.
 *
 * // OOM: Composition – Patient strictly owns its PatientAllergy entries; they cannot exist without a Patient.
 */
public class PatientAllergy {

    private Long id;
    private Long patientId;
    private final String allergen;
    private final String severity;
    private final LocalDateTime createdAt;

    public PatientAllergy(Long id, Long patientId, String allergen, String severity, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.allergen = Objects.requireNonNull(allergen, "Allergen must not be null");
        this.severity = severity != null ? severity : "MODERATE";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public PatientAllergy(String allergen, String severity) {
        this(null, null, allergen, severity, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getAllergen() {
        return allergen;
    }

    public String getSeverity() {
        return severity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientAllergy that)) return false;
        return Objects.equals(allergen, that.allergen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allergen);
    }

    @Override
    public String toString() {
        return allergen + " (" + severity + ")";
    }
}
