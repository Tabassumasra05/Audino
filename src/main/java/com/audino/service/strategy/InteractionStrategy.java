package com.audino.service.strategy;

import com.audino.model.InteractionRule;
import com.audino.model.Patient;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionItem;

import java.util.List;

/**
 * Strategy interface defining the contract for clinical decision support rule evaluation.
 *
 * // OOM: Strategy Pattern – encapsulates interchangeable clinical safety checking algorithms.
 */
public interface InteractionStrategy {

    /**
     * Evaluates a prescription's active items and patient medical context against clinical rules.
     *
     * @param items Current active prescription line items
     * @param patient Patient receiving therapy (allergies, chronic conditions)
     * @param rules Active clinical interaction rules
     * @return List of identified interaction alerts
     */
    List<PrescriptionAlert> check(List<PrescriptionItem> items, Patient patient, List<InteractionRule> rules);
}
