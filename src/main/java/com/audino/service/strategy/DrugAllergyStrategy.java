package com.audino.service.strategy;

import com.audino.factory.AlertFactory;
import com.audino.model.AlertLevel;
import com.audino.model.AlertType;
import com.audino.model.InteractionRule;
import com.audino.model.Medication;
import com.audino.model.Patient;
import com.audino.model.PatientAllergy;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete strategy evaluating medication contraindications against patient allergy profiles.
 *
 * // OOM: Strategy Pattern – concrete implementation for Drug-Allergy contraindication detection.
 */
public class DrugAllergyStrategy implements InteractionStrategy {

    @Override
    public List<PrescriptionAlert> check(List<PrescriptionItem> items, Patient patient, List<InteractionRule> rules) {
        List<PrescriptionAlert> alerts = new ArrayList<>();
        if (items == null || patient == null || patient.getAllergies().isEmpty()) {
            return alerts;
        }

        List<InteractionRule> allergyRules = rules != null ? rules.stream()
                .filter(r -> r.getType() == AlertType.DRUG_ALLERGY)
                .toList() : List.of();

        Set<String> reportedAlerts = new HashSet<>();

        for (PrescriptionItem item : items) {
            Medication med = item.getMedication();
            if (med == null) continue;

            for (PatientAllergy allergy : patient.getAllergies()) {
                String allergen = allergy.getAllergen().trim().toLowerCase();

                // 1. Direct ingredient or drug name match
                boolean directMatch = matches(med, allergen);
                if (directMatch) {
                    String key = "DIRECT:" + med.getId() + ":" + allergen;
                    if (reportedAlerts.add(key)) {
                        String msg = "ALLERGY WARNING: Patient has a recorded '" + allergy.getAllergen() +
                                     "' allergy. " + med.getBrandName() + " (" + med.getGenericName() +
                                     ") contains cross-reactive allergens.";
                        alerts.add(AlertFactory.createAlert(AlertLevel.CRITICAL, AlertType.DRUG_ALLERGY, msg));
                    }
                    continue;
                }

                // 2. Rule-based interaction match (e.g. Sulfa -> Celecoxib)
                for (InteractionRule rule : allergyRules) {
                    boolean allergenMatchesK1 = allergen.contains(rule.getKeyword1().toLowerCase()) ||
                                                rule.getKeyword1().toLowerCase().contains(allergen);
                    boolean medMatchesK2 = matches(med, rule.getKeyword2());

                    if (allergenMatchesK1 && medMatchesK2) {
                        String key = "RULE:" + rule.getId() + ":" + med.getId();
                        if (reportedAlerts.add(key)) {
                            String msg = "ALLERGY CONTRAINDICATION: Patient has confirmed '" + allergy.getAllergen() +
                                         "' allergy. " + rule.getDescription();
                            if (rule.getRecommendation() != null && !rule.getRecommendation().isBlank()) {
                                msg += " [Recommendation: " + rule.getRecommendation() + "]";
                            }
                            alerts.add(AlertFactory.createAlert(rule.getSeverity(), AlertType.DRUG_ALLERGY, msg));
                        }
                    }
                }
            }
        }
        return alerts;
    }

    private boolean matches(Medication med, String keyword) {
        if (med == null || keyword == null) return false;
        String kw = keyword.trim().toLowerCase();
        if (med.getGenericName().toLowerCase().contains(kw) || med.getBrandName().toLowerCase().contains(kw)) {
            return true;
        }
        for (String ing : med.getIngredients()) {
            if (ing.toLowerCase().contains(kw) || kw.contains(ing.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
