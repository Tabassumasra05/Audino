package com.audino.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liquid, syrup, or oral suspension formulation.
 * Dosage examples: "5ml", "10ml", "15ml", "2.5ml", "1 tsp", "2 tsp", "1 tbsp".
 */
public class LiquidMedication extends Medication {

    private static final Pattern ML_PATTERN = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)\\s*ml$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TSP_PATTERN = Pattern.compile("^([1-9][0-9]*)\\s*tsp$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TBSP_PATTERN = Pattern.compile("^([1-9][0-9]*)\\s*tbsp$", Pattern.CASE_INSENSITIVE);

    public LiquidMedication(Long id, String genericName, String brandName, String rxNormCode,
                            String strength, List<String> ingredients, BigDecimal pricePerUnit, LocalDateTime createdAt) {
        super(id, genericName, brandName, rxNormCode, MedicationType.LIQUID, strength, ingredients, pricePerUnit, createdAt);
    }

    public LiquidMedication(Long id, String genericName, String brandName, String rxNormCode,
                            String strength, List<String> ingredients, BigDecimal pricePerUnit) {
        this(id, genericName, brandName, rxNormCode, strength, ingredients, pricePerUnit, LocalDateTime.now());
    }

    @Override
    public boolean isValidDosage(String dosage) {
        if (dosage == null || dosage.isBlank()) return false;
        String trimmed = dosage.trim();
        Matcher m1 = ML_PATTERN.matcher(trimmed);
        if (m1.matches()) {
            double ml = Double.parseDouble(m1.group(1));
            return ml >= 0.5 && ml <= 100.0;
        }
        Matcher m2 = TSP_PATTERN.matcher(trimmed);
        if (m2.matches()) {
            int tsp = Integer.parseInt(m2.group(1));
            return tsp >= 1 && tsp <= 6;
        }
        Matcher m3 = TBSP_PATTERN.matcher(trimmed);
        if (m3.matches()) {
            int tbsp = Integer.parseInt(m3.group(1));
            return tbsp >= 1 && tbsp <= 3;
        }
        return false;
    }

    @Override
    public String getUnitLabel() {
        return "ml";
    }

    @Override
    public int calculateUnits(String dosage, int timesPerDay, int days) {
        if (dosage == null || dosage.isBlank() || days <= 0 || timesPerDay <= 0) return 0;
        double mlPerDose = 5.0; // default 5ml
        String trimmed = dosage.trim();

        Matcher m1 = ML_PATTERN.matcher(trimmed);
        if (m1.matches()) {
            mlPerDose = Double.parseDouble(m1.group(1));
        } else {
            Matcher m2 = TSP_PATTERN.matcher(trimmed);
            if (m2.matches()) {
                mlPerDose = Integer.parseInt(m2.group(1)) * 5.0;
            } else {
                Matcher m3 = TBSP_PATTERN.matcher(trimmed);
                if (m3.matches()) {
                    mlPerDose = Integer.parseInt(m3.group(1)) * 15.0;
                }
            }
        }
        return (int) Math.ceil(mlPerDose * timesPerDay * days);
    }
}
