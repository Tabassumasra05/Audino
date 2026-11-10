package com.audino;

import com.audino.dao.AuditLogDAO;
import com.audino.dao.InteractionRuleDAO;
import com.audino.dao.PrescriptionDAO;
import com.audino.exception.CriticalInteractionException;
import com.audino.exception.ValidationException;
import com.audino.model.AlertLevel;
import com.audino.model.AlertType;
import com.audino.model.AuditLog;
import com.audino.model.Doctor;
import com.audino.model.Frequency;
import com.audino.model.InteractionRule;
import com.audino.model.Medication;
import com.audino.model.Patient;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionItem;
import com.audino.model.PrescriptionStatus;
import com.audino.model.TabletMedication;
import com.audino.service.AuditService;
import com.audino.service.PrescriptionService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit Test: PrescriptionService & Mandatory Clinical Override")
class PrescriptionServiceTest {

    private PrescriptionService prescriptionService;
    private MockPrescriptionDAO prescriptionDAO;
    private MockAuditLogDAO auditLogDAO;
    private MockInteractionRuleDAO ruleDAO;

    private Doctor loggedInDoctor;
    private Medication warfarin;
    private Medication ibuprofen;
    private Medication paracetamol;

    @BeforeEach
    void setUp() {
        prescriptionDAO = new MockPrescriptionDAO();
        auditLogDAO = new MockAuditLogDAO();
        ruleDAO = new MockInteractionRuleDAO();

        AuditService auditService = new AuditService(auditLogDAO);
        SafetyEngine safetyEngine = new SafetyEngine(ruleDAO);

        prescriptionService = new PrescriptionService(prescriptionDAO, safetyEngine, auditService);

        loggedInDoctor = new Doctor("dr_mehta", "hash", "Dr. Arjun Mehta", "Internal Medicine", "MCI-48291");

        warfarin = new TabletMedication(1L, "Warfarin", "Warf 5", "11289", "5mg", List.of("Warfarin"), new BigDecimal("85.00"));
        ibuprofen = new TabletMedication(2L, "Ibuprofen", "Brufen 400", "5640", "400mg", List.of("Ibuprofen"), new BigDecimal("25.00"));
        paracetamol = new TabletMedication(3L, "Paracetamol", "Dolo 650", "3222", "650mg", List.of("Paracetamol"), new BigDecimal("3.00"));

        ruleDAO.addRule(new InteractionRule(1L, AlertType.DRUG_DRUG, "Warfarin", "Ibuprofen",
                AlertLevel.CRITICAL, "High hemorrhage risk", "Avoid concurrent use"));
    }

    @Test
    @DisplayName("Attempting to approve a prescription with an unjustified CRITICAL alert throws CriticalInteractionException")
    void testUnjustifiedCriticalAlertBlocksApproval() {
        Prescription rx = new Prescription(100L, loggedInDoctor.getUsername(), 1L);
        rx.addItem(new PrescriptionItem(warfarin, "1 tablet", Frequency.ONCE_DAILY, 10, "After food"));
        rx.addItem(new PrescriptionItem(ibuprofen, "1 tablet", Frequency.TWICE_DAILY, 5, "After food"));

        // Trigger safety engine to attach CRITICAL alert
        rx.addAlert(new PrescriptionAlert(AlertLevel.CRITICAL, AlertType.DRUG_DRUG, "Warfarin + Ibuprofen: Risk of severe hemorrhage"));

        assertThat(rx.hasUnjustifiedCriticalAlerts()).isTrue();

        assertThatThrownBy(() -> prescriptionService.savePrescription(rx, loggedInDoctor))
                .isInstanceOf(CriticalInteractionException.class)
                .hasMessageContaining("Cannot approve prescription with unresolved CRITICAL interactions");

        assertThat(prescriptionDAO.savedPrescription).isNull();
    }

    @Test
    @DisplayName("Providing an override justification (< 15 chars) is rejected as invalid")
    void testShortOverrideJustificationRejected() {
        PrescriptionAlert alert = new PrescriptionAlert(AlertLevel.CRITICAL, AlertType.DRUG_DRUG, "Conflict");

        // Attempting to record override with note < 15 chars
        assertThatThrownBy(() -> alert.recordOverride("short note", loggedInDoctor.getUsername()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 15 characters");

        assertThat(alert.isOverridden()).isFalse();
    }

    @Test
    @DisplayName("Providing a valid clinical justification (>= 15 chars) allows successful approval and audit recording")
    void testJustifiedCriticalAlertSavesSuccessfully() throws Exception {
        Prescription rx = new Prescription(100L, loggedInDoctor.getUsername(), 1L);
        rx.addItem(new PrescriptionItem(warfarin, "1 tablet", Frequency.ONCE_DAILY, 10, "After food"));
        rx.addItem(new PrescriptionItem(ibuprofen, "1 tablet", Frequency.TWICE_DAILY, 5, "After food"));

        PrescriptionAlert alert = new PrescriptionAlert(10L, 1L, AlertLevel.CRITICAL, AlertType.DRUG_DRUG,
                "Warfarin + Ibuprofen: Severe bleeding risk", false, null, null, null);
        rx.addAlert(alert);

        // Doctor provides valid justification (>= 15 chars)
        alert.recordOverride("Daily INR monitored; indicated for acute co-morbidity under hospital observation", loggedInDoctor.getUsername());

        assertThat(rx.hasUnjustifiedCriticalAlerts()).isFalse();

        Prescription saved = prescriptionService.savePrescription(rx, loggedInDoctor);

        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(PrescriptionStatus.APPROVED);
        assertThat(saved.getDoctorUsername()).isEqualTo(loggedInDoctor.getUsername());
        assertThat(saved.getPrescriber()).isEqualTo(loggedInDoctor);

        // Verify override was logged to audit trail
        assertThat(auditLogDAO.savedLogs).anyMatch(log ->
                log.getOperationType().equals("CRITICAL_OVERRIDE") &&
                log.getOperatorUsername().equals(loggedInDoctor.getUsername()) &&
                log.getDetails().contains("Daily INR monitored"));
    }

    @Test
    @DisplayName("Prescription cost and total in INR (₹) are calculated correctly")
    void testCostCalculationAccuracy() {
        Prescription rx = new Prescription(100L, loggedInDoctor.getUsername(), 1L);
        // Dolo 650: 1 tablet, Twice daily for 5 days = 10 tablets @ 3.00 = 30.00
        rx.addItem(new PrescriptionItem(paracetamol, "1 tablet", Frequency.TWICE_DAILY, 5, "After food"));
        // Brufen 400: 1 tablet, Once daily for 4 days = 4 tablets @ 25.00 = 100.00
        rx.addItem(new PrescriptionItem(ibuprofen, "1 tablet", Frequency.ONCE_DAILY, 4, "After food"));

        rx.recalculateTotal();

        // 30.00 + 100.00 = 130.00
        assertThat(rx.getTotalCost()).isEqualByComparingTo(new BigDecimal("130.00"));
    }

    @Test
    @DisplayName("Empty prescription with no items cannot be approved")
    void testEmptyPrescriptionCannotBeApproved() {
        Prescription rx = new Prescription(100L, loggedInDoctor.getUsername(), 1L);

        assertThatThrownBy(() -> prescriptionService.savePrescription(rx, loggedInDoctor))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot save an empty prescription");
    }

    // Mocks
    private static class MockPrescriptionDAO implements PrescriptionDAO {
        Prescription savedPrescription;

        @Override
        public Prescription saveWithTransaction(Prescription prescription) {
            prescription.setId(999L);
            this.savedPrescription = prescription;
            return prescription;
        }

        @Override public Optional<Prescription> findById(Long id) { return Optional.ofNullable(savedPrescription); }
        @Override public Optional<Prescription> findByTokenId(Long tokenId) { return Optional.empty(); }
        @Override public List<Prescription> findByPatientId(Long patientId) { return List.of(); }
        @Override public int countTodayPrescriptions() { return 1; }
        @Override public int countTodayCriticalOverrides() { return 1; }
    }

    private static class MockAuditLogDAO implements AuditLogDAO {
        final List<AuditLog> savedLogs = new ArrayList<>();

        @Override
        public AuditLog save(AuditLog log) {
            savedLogs.add(log);
            return log;
        }
        @Override public List<AuditLog> findRecent(int limit) { return savedLogs; }
        @Override public List<AuditLog> search(String operator, String operationType, LocalDate startDate, LocalDate endDate, int limit) { return savedLogs; }
    }

    private static class MockInteractionRuleDAO implements InteractionRuleDAO {
        private final List<InteractionRule> rules = new ArrayList<>();
        void addRule(InteractionRule r) { rules.add(r); }
        @Override public Optional<InteractionRule> findById(Long id) { return Optional.empty(); }
        @Override public List<InteractionRule> findAll() { return rules; }
        @Override public List<InteractionRule> findByType(AlertType type) { return rules; }
        @Override public InteractionRule save(InteractionRule rule) { return rule; }
        @Override public void update(InteractionRule rule) {}
        @Override public void delete(Long id) {}
    }
}
