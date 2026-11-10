package com.audino.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract pharmaceutical entity representing a standardized formulary entry.
 *
 * // OOM: Inheritance & Abstraction – base class defining core pharmacology properties and polymorphic dosage calculation.
 */
public abstract class Medication {

    private Long id;
    private final String genericName;
    private final String brandName;
    private final String rxNormCode;
    private final MedicationType type;
    private final String strength;
    private final List<String> ingredients = new ArrayList<>();
    private BigDecimal pricePerUnit;
    private final LocalDateTime createdAt;

    protected Medication(Long id, String genericName, String brandName, String rxNormCode,
                         MedicationType type, String strength, List<String> ingredients,
                         BigDecimal pricePerUnit, LocalDateTime createdAt) {
        this.id = id;
        this.genericName = Objects.requireNonNull(genericName, "Generic name must not be null");
        this.brandName = Objects.requireNonNull(brandName, "Brand name must not be null");
        this.rxNormCode = rxNormCode;
        this.type = Objects.requireNonNull(type, "Medication type must not be null");
        this.strength = Objects.requireNonNull(strength, "Strength must not be null");
        if (ingredients != null) {
            this.ingredients.addAll(ingredients);
        }
        this.pricePerUnit = pricePerUnit != null ? pricePerUnit.setScale(2, RoundingMode.HALF_UP) : null;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * Polymorphically validates that an entered dosage string is physically realistic
     * and syntactically valid for this medication delivery type.
     *
     * // OOM: Polymorphism – each medication subclass applies distinct physical dosage validation grammar.
     *
     * @param dosage User entered dosage string (e.g. "1 tablet", "5ml", "1 vial")
     * @return true if valid, false otherwise
     */
    public abstract boolean isValidDosage(String dosage);

    /**
     * Retrieves the display unit label (e.g. "tablets", "capsules", "ml", "vials").
     *
     * @return Display unit string
     */
    public abstract String getUnitLabel();

    /**
     * Calculates the total physical units required for the prescription course.
     *
     * @param dosage Prescribed dosage per intake
     * @param timesPerDay Frequency multiplier
     * @param days Duration of therapy
     * @return Total integer units required
     */
    public abstract int calculateUnits(String dosage, int timesPerDay, int days);

    /**
     * Polymorphic cost calculation based on calculated units and unit price in INR (₹).
     *
     * // OOM: Polymorphism – cost calculation calls medication.calculateUnits(...) polymorphically.
     *
     * @param dosage Prescribed dosage
     * @param timesPerDay Frequency count
     * @param days Duration in days
     * @return Total line cost in INR
     */
    public BigDecimal calculateCost(String dosage, int timesPerDay, int days) {
        if (pricePerUnit == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int units = calculateUnits(dosage, timesPerDay, days);
        return pricePerUnit.multiply(BigDecimal.valueOf(units)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCost(int totalUnits) {
        if (pricePerUnit == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return pricePerUnit.multiply(BigDecimal.valueOf(totalUnits)).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGenericName() {
        return genericName;
    }

    public String getBrandName() {
        return brandName;
    }

    public String getRxNormCode() {
        return rxNormCode;
    }

    public MedicationType getType() {
        return type;
    }

    public String getDosageHint() {
        return type != null ? type.getDefaultDosageHint() : "1 unit";
    }

    public String getStrength() {
        return strength;
    }

    // OOM: Encapsulation – returns unmodifiable defensive copy of active ingredients collection
    public List<String> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit != null ? pricePerUnit.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medication that)) return false;
        return Objects.equals(genericName, that.genericName) &&
               Objects.equals(brandName, that.brandName) &&
               Objects.equals(strength, that.strength);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genericName, brandName, strength);
    }

    @Override
    public String toString() {
        return brandName + " (" + genericName + " " + strength + ")";
    }
}
