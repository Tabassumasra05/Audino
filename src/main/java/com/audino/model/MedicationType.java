package com.audino.model;

/**
 * Pharmaceutical delivery mechanisms.
 */
public enum MedicationType {
    TABLET("Tablet", "tablets"),
    CAPSULE("Capsule", "capsules"),
    LIQUID("Liquid / Oral Suspension", "ml"),
    INJECTION("Injectable / Vial", "vials");

    private final String displayName;
    private final String defaultUnit;

    MedicationType(String displayName, String defaultUnit) {
        this.displayName = displayName;
        this.defaultUnit = defaultUnit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public String getDefaultDosageHint() {
        return switch (this) {
            case TABLET -> "1 tablet";
            case CAPSULE -> "1 capsule";
            case LIQUID -> "5 ml";
            case INJECTION -> "1 vial";
        };
    }
}
