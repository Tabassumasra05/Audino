package com.audino.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parenteral injectable formulation (IV / IM / SC).
 * Dosage examples: "1 vial", "2 vials", "1 ampoule", "2ml", "1 vial/day".
 */
public class InjectionMedication extends Medication {

    private static final Pattern VIAL_PATTERN = Pattern.compile("^([1-9][0-9]*)\\s*(?:vial|vials|ampoule|ampoules)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ML_PATTERN = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)\\s*ml$", Pattern.CASE_INSENSITIVE);

    public InjectionMedication(Long id, String genericName, String brandName, String rxNormCode,
                               String strength, List<String> ingredients, BigDecimal pricePerUnit, LocalDateTime createdAt) {
        super(id, genericName, brandName, rxNormCode, MedicationType.INJECTION, strength, ingredients, pricePerUnit, createdAt);
    }

    public InjectionMedication(Long id, String genericName, String brandName, String rxNormCode,
                               String strength, List<String> ingredients, BigDecimal pricePerUnit) {
        this(id, genericName, brandName, rxNormCode, strength, ingredients, pricePerUnit, LocalDateTime.now());
    }

    @Override
    public boolean isValidDosage(String dosage) {
        if (dosage == null || dosage.isBlank()) return false;
        String trimmed = dosage.trim();
        Matcher m1 = VIAL_PATTERN.matcher(trimmed);
        if (m1.matches()) {
            int vials = Integer.parseInt(m1.group(1));
            return vials >= 1 && vials <= 5;
        }
        Matcher m2 = ML_PATTERN.matcher(trimmed);
        if (m2.matches()) {
            double ml = Double.parseDouble(m2.group(1));
            return ml >= 0.5 && ml <= 50.0;
        }
        return false;
    }

    @Override
    public String getUnitLabel() {
        return "vials";
    }

    @Override
    public int calculateUnits(String dosage, int timesPerDay, int days) {
        if (dosage == null || dosage.isBlank() || days <= 0 || timesPerDay <= 0) return 0;
        int vialsPerDose = 1;
        Matcher m = VIAL_PATTERN.matcher(dosage.trim());
        if (m.matches()) {
            vialsPerDose = Integer.parseInt(m.group(1));
        }
        return vialsPerDose * timesPerDay * days;
    }
}
