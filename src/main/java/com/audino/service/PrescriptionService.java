package com.audino.service;

import com.audino.dao.PrescriptionDAO;
import com.audino.exception.CriticalInteractionException;
import com.audino.exception.ValidationException;
import com.audino.model.Doctor;
import com.audino.model.Patient;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionStatus;
import com.audino.service.strategy.SafetyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service managing prescription authoring, safety engine execution,
 * critical override enforcement, and atomic persistence.
 *
 * // OOM: Single Responsibility Principle (SRP) – solely responsible for prescription lifecycle orchestration.
 */
public class PrescriptionService {

    private static final Logger logger = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionDAO prescriptionDAO;
    private final SafetyEngine safetyEngine;
    private final AuditService auditService;

    public PrescriptionService(PrescriptionDAO prescriptionDAO, SafetyEngine safetyEngine, AuditService auditService) {
        this.prescriptionDAO = Objects.requireNonNull(prescriptionDAO, "PrescriptionDAO must not be null");
        this.safetyEngine = Objects.requireNonNull(safetyEngine, "SafetyEngine must not be null");
        this.auditService = Objects.requireNonNull(auditService, "AuditService must not be null");
    }

    /**
     * Executes the Clinical Decision Support safety engine on the prescription items.
     * Updates the prescription's alert collection.
     *
     * @param prescription The prescription being authored
     * @param patient The patient receiving medication
     * @return List of identified safety alerts
     */
    public List<PrescriptionAlert> checkSafety(Prescription prescription, Patient patient) {
        Objects.requireNonNull(prescription, "Prescription must not be null");
        List<PrescriptionAlert> alerts = safetyEngine.evaluate(prescription, patient);

        prescription.clearAlerts();
        for (PrescriptionAlert a : alerts) {
            prescription.addAlert(a);
        }
        return alerts;
    }

    /**
     * Approves and persists an active prescription.
     * Enforces clinical safety laws: throws CriticalInteractionException if any CRITICAL
     * interaction alert lacks a documented clinical justification note (>= 15 chars).
     *
     * @param prescription The prescription to approve
     * @param doctor The logged-in prescribing doctor
     * @return The persisted Prescription with generated ID
     * @throws CriticalInteractionException if unresolved critical alerts remain
     * @throws ValidationException if prescription contains no items or invalid state
     */
    public Prescription savePrescription(Prescription prescription, Doctor doctor)
            throws CriticalInteractionException, ValidationException {
        Objects.requireNonNull(prescription, "Prescription must not be null");
        Objects.requireNonNull(doctor, "Prescribing doctor must not be null");

        if (prescription.getItems().isEmpty()) {
            throw new ValidationException("Cannot save an empty prescription with no medication items");
        }

        // Enforce Prescriber = the logged-in doctor
        prescription.setDoctorUsername(doctor.getUsername());
        prescription.setPrescriber(doctor);

        // Enforce recalculation of financial totals
        prescription.recalculateTotal();

        // Safety Gate: Check for unjustified CRITICAL interactions
        if (prescription.hasUnjustifiedCriticalAlerts()) {
            StringBuilder sb = new StringBuilder("Cannot approve prescription with unresolved CRITICAL interactions:\n");
            for (PrescriptionAlert alert : prescription.getAlerts()) {
                if (alert.isCritical() && !alert.isOverridden()) {
                    sb.append("• ").append(alert.getMessage()).append("\n");
                }
            }
            sb.append("\nYou must either remove the conflicting drug or provide a documented clinical justification (min 15 characters).");
            logger.warn("Prescription approval blocked: unjustified critical interaction detected for doctor '{}'.", doctor.getUsername());
            throw new CriticalInteractionException(sb.toString().trim());
        }

        // Set status to APPROVED
        prescription.setStatus(PrescriptionStatus.APPROVED);

        // Persist atomically via transaction
        Prescription saved = prescriptionDAO.saveWithTransaction(prescription);

        // Audit overrides
        for (PrescriptionAlert alert : saved.getAlerts()) {
            if (alert.isCritical() && alert.isOverridden()) {
                auditService.logCriticalOverride(doctor.getUsername(), alert, alert.getOverrideNote());
            }
        }

        // Audit approved prescription
        auditService.logPrescriptionSaved(doctor.getUsername(), saved.getId(), saved.getTotalCost());
        logger.info("Prescription #{} approved and persisted successfully by Dr. {} (Reg: {}). Total: ₹{}",
                saved.getId(), doctor.getFullName(), doctor.getRegistrationNumber(), saved.getTotalCost());

        return saved;
    }

    public Prescription savePrescription(Prescription prescription, Patient patient, Doctor doctor)
            throws CriticalInteractionException, ValidationException {
        if (patient != null) {
            prescription.setPatient(patient);
        }
        return savePrescription(prescription, doctor);
    }

    public Optional<Prescription> findById(Long id) {
        return prescriptionDAO.findById(id);
    }

    public Optional<Prescription> findByTokenId(Long tokenId) {
        return prescriptionDAO.findByTokenId(tokenId);
    }

    public List<Prescription> findByPatientId(Long patientId) {
        return prescriptionDAO.findByPatientId(patientId);
    }
}
