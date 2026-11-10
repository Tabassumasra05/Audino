package com.audino;

import com.audino.dao.InteractionRuleDAO;
import com.audino.model.AlertLevel;
import com.audino.model.AlertType;
import com.audino.model.Frequency;
import com.audino.model.Gender;
import com.audino.model.InteractionRule;
import com.audino.model.Medication;
import com.audino.model.Patient;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionItem;
import com.audino.model.TabletMedication;
import com.audino.service.strategy.DrugAllergyStrategy;
import com.audino.service.strategy.DrugConditionStrategy;
import com.audino.service.strategy.DrugDrugStrategy;
import com.audino.service.strategy.DuplicateTherapyStrategy;
import com.audino.service.strategy.InteractionStrategy;
import com.audino.service.strategy.SafetyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Test: SafetyEngine & Strategy Pattern")
class SafetyEngineTest {

    private SafetyEngine safetyEngine;
    private MockInteractionRuleDAO ruleDAO;

    private Medication warfarin;
    private Medication ibuprofen;
    private Medication aspirin;
    private Medication amoxicillin;
    private Medication paracetamol;
    private Medication dolo;
    private Medication metformin;

    private Patient cleanPatient;
    private Patient penicillinAllergicPatient;
    private Patient pepticUlcerPatient;

    @BeforeEach
    void setUp() {
        ruleDAO = new MockInteractionRuleDAO();
        safetyEngine = new SafetyEngine(ruleDAO);

        // Setup test medications
        warfarin = new TabletMedication(1L, "Warfarin", "Warf 5", "11289", "5mg", List.of("Warfarin"), new BigDecimal("85.00"));
        ibuprofen = new TabletMedication(2L, "Ibuprofen", "Brufen 400", "5640", "400mg", List.of("Ibuprofen", "NSAID"), new BigDecimal("25.00"));
        aspirin = new TabletMedication(3L, "Aspirin", "Ecosprin 75", "1191", "75mg", List.of("Aspirin", "NSAID"), new BigDecimal("18.00"));
        amoxicillin = new TabletMedication(4L, "Amoxicillin", "Mox 500", "723", "500mg", List.of("Amoxicillin", "Penicillin"), new BigDecimal("75.00"));
        paracetamol = new TabletMedication(5L, "Paracetamol", "Calpol", "3222", "500mg", List.of("Paracetamol"), new BigDecimal("20.00"));
        dolo = new TabletMedication(6L, "Paracetamol", "Dolo 650", "3222", "650mg", List.of("Paracetamol"), new BigDecimal("30.00"));
        metformin = new TabletMedication(7L, "Metformin", "Glycomet 500", "6809", "500mg", List.of("Metformin"), new BigDecimal("45.00"));

        // Setup test patients
        cleanPatient = new Patient(101L, "P-101", "John", "Doe", LocalDate.of(1990, 1, 1), Gender.MALE, "9876543210", "Address", List.of(), List.of(), null);

        penicillinAllergicPatient = new Patient(102L, "P-102", "Alice", "Smith", LocalDate.of(1985, 5, 12), Gender.FEMALE, "9876543211", "Address", List.of(), List.of(), null);
        penicillinAllergicPatient.addAllergy("Penicillin", "CRITICAL");

        pepticUlcerPatient = new Patient(103L, "P-103", "Bob", "Brown", LocalDate.of(1975, 8, 20), Gender.MALE, "9876543212", "Address", List.of(), List.of(), null);
        pepticUlcerPatient.addCondition("Peptic Ulcer", LocalDate.of(2020, 1, 1));

        // Setup clinical test rules
        ruleDAO.addRule(new InteractionRule(1L, AlertType.DRUG_DRUG, "Warfarin", "Ibuprofen", AlertLevel.CRITICAL, "Severe risk of bleeding", "Avoid concurrent use"));
        ruleDAO.addRule(new InteractionRule(2L, AlertType.DRUG_ALLERGY, "Penicillin", "Amoxicillin", AlertLevel.CRITICAL, "Anaphylactic shock risk", "Use Macrolide"));
        ruleDAO.addRule(new InteractionRule(3L, AlertType.DRUG_CONDITION, "Peptic Ulcer", "Ibuprofen", AlertLevel.CRITICAL, "Mucosal perforation risk", "Use Paracetamol"));
        ruleDAO.addRule(new InteractionRule(4L, AlertType.DUPLICATE_THERAPY, "Paracetamol", "Dolo", AlertLevel.WARNING, "Duplicate acetaminophen therapy", "Consolidate dose"));
    }

    @Test
    @DisplayName("DrugDrugStrategy should detect Warfarin + Ibuprofen as CRITICAL")
    void testDrugDrugStrategyDetectsInteraction() {
        Prescription rx = new Prescription(cleanPatient.getId(), "doctor", 1L);
        rx.addItem(new PrescriptionItem(warfarin, "1 tablet", Frequency.ONCE_DAILY, 5, "After food"));
        rx.addItem(new PrescriptionItem(ibuprofen, "1 tablet", Frequency.TWICE_DAILY, 3, "After food"));

        List<PrescriptionAlert> alerts = safetyEngine.evaluate(rx, cleanPatient);

        assertThat(alerts).isNotEmpty();
        assertThat(alerts).anyMatch(a -> a.getLevel() == AlertLevel.CRITICAL &&
                                         a.getType() == AlertType.DRUG_DRUG &&
                                         a.getMessage().contains("Warfarin") &&
                                         a.getMessage().contains("Ibuprofen"));
    }

    @Test
    @DisplayName("DrugAllergyStrategy should detect Penicillin allergy contraindication for Amoxicillin")
    void testDrugAllergyStrategyDetectsContraindication() {
        Prescription rx = new Prescription(penicillinAllergicPatient.getId(), "doctor", 2L);
        rx.addItem(new PrescriptionItem(amoxicillin, "1 tablet", Frequency.THRICE_DAILY, 5, "After food"));

        List<PrescriptionAlert> alerts = safetyEngine.evaluate(rx, penicillinAllergicPatient);

        assertThat(alerts).isNotEmpty();
        assertThat(alerts).anyMatch(a -> a.getType() == AlertType.DRUG_ALLERGY &&
                                         a.getLevel() == AlertLevel.CRITICAL &&
                                         a.getMessage().contains("Penicillin"));
    }

    @Test
    @DisplayName("DrugConditionStrategy should detect Peptic Ulcer contraindication for Ibuprofen")
    void testDrugConditionStrategyDetectsContraindication() {
        Prescription rx = new Prescription(pepticUlcerPatient.getId(), "doctor", 3L);
        rx.addItem(new PrescriptionItem(ibuprofen, "1 tablet", Frequency.TWICE_DAILY, 3, "After food"));

        List<PrescriptionAlert> alerts = safetyEngine.evaluate(rx, pepticUlcerPatient);

        assertThat(alerts).isNotEmpty();
        assertThat(alerts).anyMatch(a -> a.getType() == AlertType.DRUG_CONDITION &&
                                         a.getLevel() == AlertLevel.CRITICAL &&
                                         a.getMessage().contains("Peptic Ulcer"));
    }

    @Test
    @DisplayName("DuplicateTherapyStrategy should detect duplicate Paracetamol + Dolo 650")
    void testDuplicateTherapyDetection() {
        Prescription rx = new Prescription(cleanPatient.getId(), "doctor", 4L);
        rx.addItem(new PrescriptionItem(paracetamol, "1 tablet", Frequency.TWICE_DAILY, 3, "After food"));
        rx.addItem(new PrescriptionItem(dolo, "1 tablet", Frequency.ONCE_DAILY, 3, "After food"));

        List<PrescriptionAlert> alerts = safetyEngine.evaluate(rx, cleanPatient);

        assertThat(alerts).isNotEmpty();
        assertThat(alerts).anyMatch(a -> a.getType() == AlertType.DUPLICATE_THERAPY &&
                                         a.getLevel() == AlertLevel.WARNING);
    }

    @Test
    @DisplayName("Safe combinations on a clean patient should generate ZERO alerts")
    void testSafeMedicationCombinationGeneratesNoAlerts() {
        Prescription rx = new Prescription(cleanPatient.getId(), "doctor", 5L);
        rx.addItem(new PrescriptionItem(paracetamol, "1 tablet", Frequency.TWICE_DAILY, 3, "After food"));
        rx.addItem(new PrescriptionItem(amoxicillin, "1 tablet", Frequency.THRICE_DAILY, 5, "After food"));

        List<PrescriptionAlert> alerts = safetyEngine.evaluate(rx, cleanPatient);

        assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("OCP: A new clinical interaction rule added at runtime is immediately detected")
    void testNewRuleAddedAtRuntimeIsDetected() {
        Prescription rx = new Prescription(cleanPatient.getId(), "doctor", 6L);
        rx.addItem(new PrescriptionItem(aspirin, "1 tablet", Frequency.ONCE_DAILY, 10, "After food"));
        rx.addItem(new PrescriptionItem(warfarin, "1 tablet", Frequency.ONCE_DAILY, 10, "After food"));

        // Initially without Aspirin+Warfarin rule
        List<PrescriptionAlert> initialAlerts = safetyEngine.evaluate(rx, cleanPatient);
        assertThat(initialAlerts).isEmpty();

        // Add new rule at runtime (no application reboot)
        ruleDAO.addRule(new InteractionRule(10L, AlertType.DRUG_DRUG, "Warfarin", "Aspirin",
                AlertLevel.CRITICAL, "Synergistic bleeding risk", "Requires strict INR monitoring"));

        List<PrescriptionAlert> updatedAlerts = safetyEngine.evaluate(rx, cleanPatient);
        assertThat(updatedAlerts).isNotEmpty();
        assertThat(updatedAlerts).anyMatch(a -> a.getLevel() == AlertLevel.CRITICAL &&
                                                a.getMessage().contains("Warfarin") &&
                                                a.getMessage().contains("Aspirin"));
    }

    @Test
    @DisplayName("OCP: Registering a custom strategy dynamically integrates into the evaluation loop")
    void testCustomStrategyRegistration() {
        InteractionStrategy customStrategy = (items, patient, rules) -> {
            List<PrescriptionAlert> res = new ArrayList<>();
            if (items.size() > 3) {
                res.add(new PrescriptionAlert(AlertLevel.INFO, AlertType.DUPLICATE_THERAPY, "Polypharmacy Alert: > 3 drugs prescribed"));
            }
            return res;
        };

        safetyEngine.registerStrategy(customStrategy);
        assertThat(safetyEngine.getStrategies()).contains(customStrategy);

        Prescription rx = new Prescription(cleanPatient.getId(), "doctor", 7L);
        rx.addItem(new PrescriptionItem(paracetamol, "1", Frequency.ONCE_DAILY, 1, ""));
        rx.addItem(new PrescriptionItem(amoxicillin, "1", Frequency.ONCE_DAILY, 1, ""));
        rx.addItem(new PrescriptionItem(metformin, "1", Frequency.ONCE_DAILY, 1, ""));
        rx.addItem(new PrescriptionItem(warfarin, "1", Frequency.ONCE_DAILY, 1, ""));

        List<PrescriptionAlert> alerts = safetyEngine.evaluate(rx, cleanPatient);
        assertThat(alerts).anyMatch(a -> a.getMessage().contains("Polypharmacy Alert"));
    }

    private static class MockInteractionRuleDAO implements InteractionRuleDAO {
        private final List<InteractionRule> rules = new ArrayList<>();

        void addRule(InteractionRule rule) {
            rules.add(rule);
        }

        @Override
        public Optional<InteractionRule> findById(Long id) {
            return rules.stream().filter(r -> r.getId().equals(id)).findFirst();
        }

        @Override
        public List<InteractionRule> findAll() {
            return new ArrayList<>(rules);
        }

        @Override
        public List<InteractionRule> findByType(AlertType type) {
            return rules.stream().filter(r -> r.getType() == type).toList();
        }

        @Override
        public InteractionRule save(InteractionRule rule) {
            rules.add(rule);
            return rule;
        }

        @Override
        public void update(InteractionRule rule) {}

        @Override
        public void delete(Long id) {
            rules.removeIf(r -> r.getId().equals(id));
        }
    }
}
