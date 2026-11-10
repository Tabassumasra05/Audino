package com.audino.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hard or soft gelatin oral capsule formulation.
 * Dosage examples: "1 capsule", "2 capsules", "1", "2". Capsules cannot be split.
 */
public class CapsuleMedication extends Medication {

    private static final Pattern CAPSULE_PATTERN = Pattern.compile("^([1-9][0-9]*)\\s*(?:cap|capsule|capsules)?$", Pattern.CASE_INSENSITIVE);

    public CapsuleMedication(Long id, String genericName, String brandName, String rxNormCode,
                             String strength, List<String> ingredients, BigDecimal pricePerUnit, LocalDateTime createdAt) {
        super(id, genericName, brandName, rxNormCode, MedicationType.CAPSULE, strength, ingredients, pricePerUnit, createdAt);
    }

    public CapsuleMedication(Long id, String genericName, String brandName, String rxNormCode,
                             String strength, List<String> ingredients, BigDecimal pricePerUnit) {
        this(id, genericName, brandName, rxNormCode, strength, ingredients, pricePerUnit, LocalDateTime.now());
    }

    @Override
    public boolean isValidDosage(String dosage) {
        if (dosage == null || dosage.isBlank()) return false;
        Matcher m = CAPSULE_PATTERN.matcher(dosage.trim());
        if (m.matches()) {
            int count = Integer.parseInt(m.group(1));
            return count >= 1 && count <= 6;
        }
        return false;
    }

    @Override
    public String getUnitLabel() {
        return "capsules";
    }

    @Override
    public int calculateUnits(String dosage, int timesPerDay, int days) {
        if (dosage == null || dosage.isBlank() || days <= 0 || timesPerDay <= 0) return 0;
        int capsulesPerDose = 1;
        Matcher m = CAPSULE_PATTERN.matcher(dosage.trim());
        if (m.matches()) {
            capsulesPerDose = Integer.parseInt(m.group(1));
        }
        return capsulesPerDose * timesPerDay * days;
    }
}
