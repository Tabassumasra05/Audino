package com.audino.model;

/**
 * Categories of adverse drug interactions and contraindications.
 */
public enum AlertType {
    DRUG_DRUG("Drug-Drug Interaction"),
    DRUG_ALLERGY("Drug-Allergy Contraindication"),
    DRUG_CONDITION("Drug-Condition Contraindication"),
    DUPLICATE_THERAPY("Duplicate Therapy Warning");

    private final String description;

    AlertType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
