package com.audino.service.strategy;

import com.audino.factory.AlertFactory;
import com.audino.model.AlertType;
import com.audino.model.InteractionRule;
import com.audino.model.Medication;
import com.audino.model.Patient;
import com.audino.model.PatientCondition;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete strategy evaluating medication contraindications against patient chronic medical conditions.
 *
 * // OOM: Strategy Pattern – concrete implementation for Drug-Condition contraindication checking.
 */
public class DrugConditionStrategy implements InteractionStrategy {

    @Override
    public List<PrescriptionAlert> check(List<PrescriptionItem> items, Patient patient, List<InteractionRule> rules) {
        List<PrescriptionAlert> alerts = new ArrayList<>();
        if (items == null || patient == null || patient.getConditions().isEmpty() || rules == null) {
            return alerts;
        }

        List<InteractionRule> condRules = rules.stream()
                .filter(r -> r.getType() == AlertType.DRUG_CONDITION)
                .toList();

        Set<String> reportedAlerts = new HashSet<>();

        for (PrescriptionItem item : items) {
            Medication med = item.getMedication();
            if (med == null) continue;

            for (PatientCondition condition : patient.getConditions()) {
                String condName = condition.getConditionName().trim().toLowerCase();

                for (InteractionRule rule : condRules) {
                    boolean condMatches = condName.contains(rule.getKeyword1().toLowerCase()) ||
                                          rule.getKeyword1().toLowerCase().contains(condName);
                    boolean medMatches = matches(med, rule.getKeyword2());

                    if (condMatches && medMatches) {
                        String key = rule.getId() + ":" + med.getId() + ":" + condition.getId();
                        if (reportedAlerts.add(key)) {
                            String msg = "CONDITION CONTRAINDICATION: Patient diagnosed with '" + condition.getConditionName() +
                                         "'. " + rule.getDescription();
                            if (rule.getRecommendation() != null && !rule.getRecommendation().isBlank()) {
                                msg += " [Recommendation: " + rule.getRecommendation() + "]";
                            }
                            alerts.add(AlertFactory.createAlert(rule.getSeverity(), AlertType.DRUG_CONDITION, msg));
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
            if (ing.toLowerCase().contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
