package com.audino.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Core clinical entity representing an outpatient electronic prescription.
 *
 * // OOM: Composition & Encapsulation – owns PrescriptionItem and PrescriptionAlert instances; maintains financial and safety invariants.
 */
public class Prescription {

    private Long id;
    private final Long patientId;
    private String doctorUsername;
    private final Long tokenId;
    private PrescriptionStatus status;
    private BigDecimal totalCost;
    private final LocalDateTime createdAt;

    // OOM: Composition – Prescription owns its items and alerts; they are deleted with it (ON DELETE CASCADE)
    private final List<PrescriptionItem> items = new ArrayList<>();
    private final List<PrescriptionAlert> alerts = new ArrayList<>();

    // Navigational associations
    private Patient patient;
    private Doctor prescriber;
    private Token token;

    public Prescription(Long id, Long patientId, String doctorUsername, Long tokenId,
                        PrescriptionStatus status, BigDecimal totalCost, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = Objects.requireNonNull(patientId, "Patient ID must not be null");
        this.doctorUsername = Objects.requireNonNull(doctorUsername, "Doctor username must not be null");
        this.tokenId = Objects.requireNonNull(tokenId, "Token ID must not be null");
        this.status = status != null ? status : PrescriptionStatus.DRAFT;
        this.totalCost = totalCost != null ? totalCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Prescription(Long patientId, String doctorUsername, Long tokenId) {
        this(null, patientId, doctorUsername, tokenId, PrescriptionStatus.DRAFT, BigDecimal.ZERO, LocalDateTime.now());
    }

    // OOM: Composition – items are created and added only through prescription
    public void addItem(PrescriptionItem item) {
        Objects.requireNonNull(item, "Prescription item must not be null");
        item.setPrescriptionId(this.id);
        this.items.add(item);
        recalculateTotal();
    }

    public boolean removeItem(PrescriptionItem item) {
        boolean removed = this.items.remove(item);
        if (removed) {
            recalculateTotal();
        }
        return removed;
    }

    public void addAlert(PrescriptionAlert alert) {
        Objects.requireNonNull(alert, "Prescription alert must not be null");
        alert.setPrescriptionId(this.id);
        this.alerts.add(alert);
    }

    public void clearAlerts() {
        this.alerts.clear();
    }

    /**
     * Recalculates total cost in INR (₹) by aggregating all line items.
     */
    public void recalculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (PrescriptionItem item : items) {
            if (item.getLineCost() != null) {
                sum = sum.add(item.getLineCost());
            }
        }
        this.totalCost = sum.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Evaluates whether there are any active CRITICAL alerts that have not been
     * justified by the physician with a valid override note (>= 15 chars).
     *
     * @return true if an unjustified critical alert exists, false otherwise
     */
    public boolean hasUnjustifiedCriticalAlerts() {
        for (PrescriptionAlert alert : alerts) {
            if (alert.isCritical() && !alert.isOverridden()) {
                return true;
            }
        }
        return false;
    }

    public int getCriticalAlertCount() {
        int count = 0;
        for (PrescriptionAlert alert : alerts) {
            if (alert.isCritical()) count++;
        }
        return count;
    }

    public int getWarningAlertCount() {
        int count = 0;
        for (PrescriptionAlert alert : alerts) {
            if (alert.getLevel() == AlertLevel.WARNING) count++;
        }
        return count;
    }

    public int getInfoAlertCount() {
        int count = 0;
        for (PrescriptionAlert alert : alerts) {
            if (alert.getLevel() == AlertLevel.INFO) count++;
        }
        return count;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
        for (PrescriptionItem item : items) {
            item.setPrescriptionId(id);
        }
        for (PrescriptionAlert alert : alerts) {
            alert.setPrescriptionId(id);
        }
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getDoctorUsername() {
        return doctorUsername;
    }

    public void setDoctorUsername(String doctorUsername) {
        this.doctorUsername = Objects.requireNonNull(doctorUsername);
    }

    public Long getTokenId() {
        return tokenId;
    }

    public PrescriptionStatus getStatus() {
        return status;
    }

    public void setStatus(PrescriptionStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // OOM: Encapsulation – returns unmodifiable defensive copies
    public List<PrescriptionItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<PrescriptionAlert> getAlerts() {
        return Collections.unmodifiableList(alerts);
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getPrescriber() {
        return prescriber;
    }

    public void setPrescriber(Doctor prescriber) {
        this.prescriber = prescriber;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "Prescription #" + (id != null ? id : "Draft") + " [" + status + "] - ₹" + totalCost;
    }
}
