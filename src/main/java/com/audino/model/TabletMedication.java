package com.audino.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Solid oral tablet formulation.
 * Dosage examples: "1 tablet", "2 tablets", "0.5 tablet", "1", "2", "650mg".
 */
public class TabletMedication extends Medication {

    private static final Pattern NUMERIC_DOSE = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)\\s*(?:tab|tablet|tablets)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MG_DOSE = Pattern.compile("^([0-9]+)\\s*mg$", Pattern.CASE_INSENSITIVE);

    public TabletMedication(Long id, String genericName, String brandName, String rxNormCode,
                            String strength, List<String> ingredients, BigDecimal pricePerUnit, LocalDateTime createdAt) {
        super(id, genericName, brandName, rxNormCode, MedicationType.TABLET, strength, ingredients, pricePerUnit, createdAt);
    }

    public TabletMedication(Long id, String genericName, String brandName, String rxNormCode,
                            String strength, List<String> ingredients, BigDecimal pricePerUnit) {
        this(id, genericName, brandName, rxNormCode, strength, ingredients, pricePerUnit, LocalDateTime.now());
    }

    @Override
    public boolean isValidDosage(String dosage) {
        if (dosage == null || dosage.isBlank()) return false;
        String trimmed = dosage.trim();
        Matcher m1 = NUMERIC_DOSE.matcher(trimmed);
        if (m1.matches()) {
            double count = Double.parseDouble(m1.group(1));
            return count >= 0.25 && count <= 10.0;
        }
        Matcher m2 = MG_DOSE.matcher(trimmed);
        if (m2.matches()) {
            int mg = Integer.parseInt(m2.group(1));
            return mg > 0 && mg <= 2000;
        }
        return false;
    }

    @Override
    public String getUnitLabel() {
        return "tablets";
    }

    @Override
    public int calculateUnits(String dosage, int timesPerDay, int days) {
        if (dosage == null || dosage.isBlank() || days <= 0 || timesPerDay <= 0) return 0;
        double tabletsPerDose = 1.0;
        Matcher m = NUMERIC_DOSE.matcher(dosage.trim());
        if (m.matches()) {
            tabletsPerDose = Double.parseDouble(m.group(1));
        }
        return (int) Math.ceil(tabletsPerDose * timesPerDay * days);
    }
}
