package com.audino.model;

/**
 * Clinical interaction alert severity levels.
 */
public enum AlertLevel {
    CRITICAL("Critical", 1),
    WARNING("Warning", 2),
    INFO("Informational", 3);

    private final String label;
    private final int severityRank;

    AlertLevel(String label, int severityRank) {
        this.label = label;
        this.severityRank = severityRank;
    }

    public String getLabel() {
        return label;
    }

    public int getSeverityRank() {
        return severityRank;
    }
}
