package com.audino.service.strategy;

import com.audino.factory.AlertFactory;
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
 * Concrete strategy detecting adverse pharmacokinetic and pharmacodynamic interactions
 * between concurrently prescribed medication pairs.
 *
 * // OOM: Strategy Pattern – concrete implementation for Drug-Drug interaction detection.
 */
public class DrugDrugStrategy implements InteractionStrategy {

    @Override
    public List<PrescriptionAlert> check(List<PrescriptionItem> items, Patient patient, List<InteractionRule> rules) {
        List<PrescriptionAlert> alerts = new ArrayList<>();
        if (items == null || items.size() < 2 || rules == null) {
            return alerts;
        }

        List<InteractionRule> ddRules = rules.stream()
                .filter(r -> r.getType() == AlertType.DRUG_DRUG)
                .toList();

        Set<String> reportedPairs = new HashSet<>();

        // Pairwise comparison of all prescribed items
        for (int i = 0; i < items.size(); i++) {
            PrescriptionItem itemA = items.get(i);
            Medication medA = itemA.getMedication();
            if (medA == null) continue;

            for (int j = i + 1; j < items.size(); j++) {
                PrescriptionItem itemB = items.get(j);
                Medication medB = itemB.getMedication();
                if (medB == null) continue;

                for (InteractionRule rule : ddRules) {
                    boolean matchForward = matches(medA, rule.getKeyword1()) && matches(medB, rule.getKeyword2());
                    boolean matchReverse = matches(medB, rule.getKeyword1()) && matches(medA, rule.getKeyword2());

                    if (matchForward || matchReverse) {
                        String pairKey = rule.getId() + ":" + Math.min(medA.getId(), medB.getId()) + "-" + Math.max(medA.getId(), medB.getId());
                        if (reportedPairs.add(pairKey)) {
                            String msg = medA.getBrandName() + " (" + medA.getGenericName() + ") + " +
                                         medB.getBrandName() + " (" + medB.getGenericName() + "): " +
                                         rule.getDescription();
                            if (rule.getRecommendation() != null && !rule.getRecommendation().isBlank()) {
                                msg += " [Recommendation: " + rule.getRecommendation() + "]";
                            }
                            alerts.add(AlertFactory.createAlert(rule.getSeverity(), AlertType.DRUG_DRUG, msg));
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
