package com.audino.service.strategy;

import com.audino.factory.AlertFactory;
import com.audino.model.AlertLevel;
import com.audino.model.AlertType;
import com.audino.model.InteractionRule;
import com.audino.model.Medication;
import com.audino.model.Patient;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete strategy detecting redundant or toxic duplicate therapeutic drug combinations.
 *
 * // OOM: Strategy Pattern – concrete implementation for Duplicate Therapy detection.
 */
public class DuplicateTherapyStrategy implements InteractionStrategy {

    @Override
    public List<PrescriptionAlert> check(List<PrescriptionItem> items, Patient patient, List<InteractionRule> rules) {
        List<PrescriptionAlert> alerts = new ArrayList<>();
        if (items == null || items.size() < 2) {
            return alerts;
        }

        List<InteractionRule> dupRules = rules != null ? rules.stream()
                .filter(r -> r.getType() == AlertType.DUPLICATE_THERAPY)
                .toList() : List.of();

        Set<String> reportedPairs = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {
            PrescriptionItem itemA = items.get(i);
            Medication medA = itemA.getMedication();
            if (medA == null) continue;

            for (int j = i + 1; j < items.size(); j++) {
                PrescriptionItem itemB = items.get(j);
                Medication medB = itemB.getMedication();
                if (medB == null) continue;

                // 1. Direct identical generic name duplication
                if (medA.getGenericName().equalsIgnoreCase(medB.getGenericName())) {
                    String pairKey = "DUP_GENERIC:" + Math.min(medA.getId(), medB.getId()) + "-" + Math.max(medA.getId(), medB.getId());
                    if (reportedPairs.add(pairKey)) {
                        String msg = "DUPLICATE THERAPY: Both " + medA.getBrandName() + " and " + medB.getBrandName() +
                                     " contain the active compound '" + medA.getGenericName() + "'. Risk of accidental overdose.";
                        alerts.add(AlertFactory.createAlert(AlertLevel.WARNING, AlertType.DUPLICATE_THERAPY, msg));
                    }
                    continue;
                }

                // 2. Overlapping active ingredients
                for (String ingA : medA.getIngredients()) {
                    for (String ingB : medB.getIngredients()) {
                        if (ingA.equalsIgnoreCase(ingB)) {
                            String pairKey = "DUP_ING:" + Math.min(medA.getId(), medB.getId()) + "-" + Math.max(medA.getId(), medB.getId()) + ":" + ingA.toLowerCase();
                            if (reportedPairs.add(pairKey)) {
                                String msg = "DUPLICATE INGREDIENT: " + medA.getBrandName() + " and " + medB.getBrandName() +
                                             " share active ingredient '" + ingA + "'. Verify aggregate dosage.";
                                alerts.add(AlertFactory.createAlert(AlertLevel.WARNING, AlertType.DUPLICATE_THERAPY, msg));
                            }
                        }
                    }
                }

                // 3. Rule-based duplicate classes (e.g. Omeprazole + Pantoprazole PPI class)
                for (InteractionRule rule : dupRules) {
                    boolean forward = matches(medA, rule.getKeyword1()) && matches(medB, rule.getKeyword2());
                    boolean reverse = matches(medB, rule.getKeyword1()) && matches(medA, rule.getKeyword2());
                    if (forward || reverse) {
                        String pairKey = "DUP_RULE:" + rule.getId() + ":" + Math.min(medA.getId(), medB.getId()) + "-" + Math.max(medA.getId(), medB.getId());
                        if (reportedPairs.add(pairKey)) {
                            String msg = "DUPLICATE THERAPY: " + rule.getDescription();
                            alerts.add(AlertFactory.createAlert(rule.getSeverity(), AlertType.DUPLICATE_THERAPY, msg));
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
