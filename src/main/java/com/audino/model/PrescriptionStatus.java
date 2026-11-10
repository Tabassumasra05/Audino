package com.audino.model;

/**
 * Lifecycle states for medical prescriptions.
 */
public enum PrescriptionStatus {
    DRAFT("Draft Prescription"),
    APPROVED("Approved & Finalized"),
    CANCELLED("Cancelled");

    private final String displayName;

    PrescriptionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
