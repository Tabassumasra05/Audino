package com.audino.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Diagnosed chronic medical condition record for a patient.
 *
 * // OOM: Composition – Patient strictly owns its PatientCondition entries.
 */
public class PatientCondition {

    private Long id;
    private Long patientId;
    private final String conditionName;
    private final LocalDate diagnosedDate;
    private final LocalDateTime createdAt;

    public PatientCondition(Long id, Long patientId, String conditionName, LocalDate diagnosedDate, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.conditionName = Objects.requireNonNull(conditionName, "Condition name must not be null");
        this.diagnosedDate = diagnosedDate;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public PatientCondition(String conditionName, LocalDate diagnosedDate) {
        this(null, null, conditionName, diagnosedDate, LocalDateTime.now());
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

    public String getConditionName() {
        return conditionName;
    }

    public LocalDate getDiagnosedDate() {
        return diagnosedDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientCondition that)) return false;
        return Objects.equals(conditionName, that.conditionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditionName);
    }

    @Override
    public String toString() {
        return conditionName;
    }
}
