package com.audino.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clinical interaction alert or contraindication generated for a prescription.
 *
 * // OOM: Composition – Prescription strictly owns its PrescriptionAlert instances.
 */
public class PrescriptionAlert {

    private Long id;
    private Long prescriptionId;
    private final AlertLevel level;
    private final AlertType type;
    private final String message;
    private boolean acknowledged;
    private String overrideNote;
    private String overriddenBy;
    private LocalDateTime overriddenAt;

    public PrescriptionAlert(Long id, Long prescriptionId, AlertLevel level, AlertType type,
                             String message, boolean acknowledged, String overrideNote,
                             String overriddenBy, LocalDateTime overriddenAt) {
        this.id = id;
        this.prescriptionId = prescriptionId;
        this.level = Objects.requireNonNull(level, "Alert level must not be null");
        this.type = Objects.requireNonNull(type, "Alert type must not be null");
        this.message = Objects.requireNonNull(message, "Alert message must not be null");
        this.acknowledged = acknowledged;
        this.overrideNote = overrideNote;
        this.overriddenBy = overriddenBy;
        this.overriddenAt = overriddenAt;
    }

    public PrescriptionAlert(AlertLevel level, AlertType type, String message) {
        this(null, null, level, type, message, false, null, null, null);
    }

    public boolean isCritical() {
        return level == AlertLevel.CRITICAL;
    }

    /**
     * Checks if this alert has been validly overridden with a clinical justification note.
     * Must be at least 15 characters according to the regulatory safety protocol.
     *
     * @return true if validly overridden, false otherwise
     */
    public boolean isOverridden() {
        return acknowledged && overrideNote != null && overrideNote.trim().length() >= 15;
    }

    /**
     * Records an official clinical override by the consulting physician.
     *
     * @param justification Note detailing clinical rationale (min 15 chars)
     * @param doctorUsername Username of the overriding physician
     */
    public void recordOverride(String justification, String doctorUsername) {
        if (justification == null || justification.trim().length() < 15) {
            throw new IllegalArgumentException("Clinical override justification must be at least 15 characters long");
        }
        this.overrideNote = justification.trim();
        this.overriddenBy = Objects.requireNonNull(doctorUsername);
        this.overriddenAt = LocalDateTime.now();
        this.acknowledged = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public AlertLevel getLevel() {
        return level;
    }

    public AlertType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public String getOverrideNote() {
        return overrideNote;
    }

    public void setOverrideNote(String overrideNote) {
        this.overrideNote = overrideNote;
    }

    public String getOverriddenBy() {
        return overriddenBy;
    }

    public void setOverriddenBy(String overriddenBy) {
        this.overriddenBy = overriddenBy;
    }

    public LocalDateTime getOverriddenAt() {
        return overriddenAt;
    }

    public void setOverriddenAt(LocalDateTime overriddenAt) {
        this.overriddenAt = overriddenAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrescriptionAlert that)) return false;
        return level == that.level && type == that.type && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, type, message);
    }

    @Override
    public String toString() {
        return "[" + level + "] " + type + ": " + message;
    }
}
