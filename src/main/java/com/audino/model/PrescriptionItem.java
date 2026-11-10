package com.audino.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Line item within a prescription detailing a specific drug, dose, frequency, and instructions.
 *
 * // OOM: Composition & Aggregation – owned by Prescription (Composition), aggregates Medication (Aggregation).
 */
public class PrescriptionItem {

    private Long id;
    private Long prescriptionId;
    private final Long medicationId;
    private String dosage;
    private Frequency frequency;
    private int durationDays;
    private String instructions;
    private BigDecimal unitCost;
    private int totalUnits;
    private BigDecimal lineCost;

    // OOM: Aggregation – aggregates a Medication that exists independently in the formulary
    private Medication medication;

    public PrescriptionItem(Long id, Long prescriptionId, Long medicationId,
                            String dosage, Frequency frequency, int durationDays,
                            String instructions, BigDecimal unitCost,
                            int totalUnits, BigDecimal lineCost) {
        this.id = id;
        this.prescriptionId = prescriptionId;
        this.medicationId = Objects.requireNonNull(medicationId, "Medication ID must not be null");
        this.dosage = Objects.requireNonNull(dosage, "Dosage must not be null");
        this.frequency = Objects.requireNonNull(frequency, "Frequency must not be null");
        this.durationDays = durationDays > 0 ? durationDays : 1;
        this.instructions = instructions;
        this.unitCost = unitCost != null ? unitCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.totalUnits = totalUnits;
        this.lineCost = lineCost != null ? lineCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public PrescriptionItem(Medication medication, String dosage, Frequency frequency, int durationDays, String instructions) {
        this.medication = Objects.requireNonNull(medication, "Medication must not be null");
        this.medicationId = medication.getId();
        this.dosage = dosage;
        this.frequency = frequency;
        this.durationDays = durationDays;
        this.instructions = instructions;
        this.unitCost = medication.getPricePerUnit() != null ? medication.getPricePerUnit() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        recalculateUnitsAndCost();
    }

    /**
     * Recalculates total units and line cost using the aggregated Medication's polymorphic logic.
     */
    public void recalculateUnitsAndCost() {
        if (medication != null) {
            this.totalUnits = medication.calculateUnits(dosage, frequency.getTimesPerDay(), durationDays);
            this.lineCost = medication.calculateCost(dosage, frequency.getTimesPerDay(), durationDays);
            this.unitCost = medication.getPricePerUnit() != null ? medication.getPricePerUnit() : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
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

    public Long getMedicationId() {
        return medicationId;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = Objects.requireNonNull(dosage);
        recalculateUnitsAndCost();
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = Objects.requireNonNull(frequency);
        recalculateUnitsAndCost();
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays > 0 ? durationDays : 1;
        recalculateUnitsAndCost();
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost != null ? unitCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public int getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(int totalUnits) {
        this.totalUnits = totalUnits;
    }

    public BigDecimal getLineCost() {
        return lineCost;
    }

    public void setLineCost(BigDecimal lineCost) {
        this.lineCost = lineCost != null ? lineCost.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Medication getMedication() {
        return medication;
    }

    public void setMedication(Medication medication) {
        this.medication = medication;
        if (medication != null) {
            recalculateUnitsAndCost();
        }
    }

    public String getMedicationName() {
        if (medication != null) {
            return medication.getBrandName() + " (" + medication.getGenericName() + " " + medication.getStrength() + ")";
        }
        return "Medication #" + medicationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrescriptionItem that)) return false;
        return Objects.equals(medicationId, that.medicationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(medicationId);
    }

    @Override
    public String toString() {
        return getMedicationName() + " - " + dosage + " " + frequency.getLabel() + " for " + durationDays + "d";
    }
}
