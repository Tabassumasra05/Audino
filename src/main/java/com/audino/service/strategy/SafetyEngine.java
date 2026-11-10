package com.audino.service.strategy;

import com.audino.dao.InteractionRuleDAO;
import com.audino.model.InteractionRule;
import com.audino.model.Patient;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Core clinical decision support orchestrator evaluating prescription safety.
 *
 * // OOM: Strategy Pattern & OCP – orchestrates interchangeable clinical validation algorithms; open for extension.
 */
public class SafetyEngine {

    private static final Logger logger = LoggerFactory.getLogger(SafetyEngine.class);

    private final List<InteractionStrategy> strategies = new ArrayList<>();
    private final InteractionRuleDAO ruleDAO;

    public SafetyEngine(InteractionRuleDAO ruleDAO) {
        this.ruleDAO = Objects.requireNonNull(ruleDAO, "InteractionRuleDAO must not be null");
        registerDefaultStrategies();
    }

    private void registerDefaultStrategies() {
        strategies.add(new DrugDrugStrategy());
        strategies.add(new DrugAllergyStrategy());
        strategies.add(new DrugConditionStrategy());
        strategies.add(new DuplicateTherapyStrategy());
        logger.info("SafetyEngine initialized with {} default clinical interaction strategies.", strategies.size());
    }

    /**
     * Registers a new clinical strategy at runtime without modifying the engine.
     * Demonstrates the Open/Closed Principle (OCP).
     *
     * @param strategy The new strategy implementation to register
     */
    public void registerStrategy(InteractionStrategy strategy) {
        if (strategy != null && !strategies.contains(strategy)) {
            strategies.add(strategy);
            logger.info("Registered custom interaction strategy: {}", strategy.getClass().getSimpleName());
        }
    }

    /**
     * Evaluates a prescription against all registered strategies polymorphically.
     *
     * @param prescription Active prescription containing line items
     * @param patient Patient medical context (allergies and chronic conditions)
     * @return Aggregated list of identified alerts
     */
    public List<PrescriptionAlert> evaluate(Prescription prescription, Patient patient) {
        if (prescription == null || prescription.getItems().isEmpty()) {
            return Collections.emptyList();
        }

        List<InteractionRule> activeRules = ruleDAO.findAll();
        List<PrescriptionAlert> aggregatedAlerts = new ArrayList<>();

        // OOM: Polymorphism – engine calls check() on every strategy without knowing its type
        for (InteractionStrategy strategy : strategies) {
            try {
                List<PrescriptionAlert> found = strategy.check(prescription.getItems(), patient, activeRules);
                if (found != null && !found.isEmpty()) {
                    aggregatedAlerts.addAll(found);
                }
            } catch (Exception e) {
                logger.error("Error executing strategy {}: {}", strategy.getClass().getSimpleName(), e.getMessage(), e);
            }
        }

        logger.debug("Safety evaluation completed: {} alerts generated across {} strategies.",
                aggregatedAlerts.size(), strategies.size());
        return aggregatedAlerts;
    }

    public List<InteractionStrategy> getStrategies() {
        return Collections.unmodifiableList(strategies);
    }
}
