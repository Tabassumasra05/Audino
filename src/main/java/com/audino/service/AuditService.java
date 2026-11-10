package com.audino.service;

import com.audino.dao.AuditLogDAO;
import com.audino.model.AuditLog;
import com.audino.model.PrescriptionAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Service managing immutable forensic audit logging for security, administrative,
 * and clinical safety override operations.
 *
 * // OOM: Single Responsibility Principle (SRP) – solely responsible for forensic event auditing.
 */
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogDAO auditLogDAO;

    public AuditService(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = Objects.requireNonNull(auditLogDAO, "AuditLogDAO must not be null");
    }

    public void logLogin(String username, boolean success, String ipAddress) {
        String operation = success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE";
        String details = success ? "User logged in successfully" : "Failed login attempt (invalid credentials or deactivated)";
        if (ipAddress != null) {
            details += " from " + ipAddress;
        }
        auditLogDAO.save(new AuditLog(username != null ? username : "ANONYMOUS", operation, "Account", username, details));
    }

    public void logCriticalOverride(String doctorUsername, PrescriptionAlert alert, String justification) {
        String details = "CRITICAL OVERRIDE: [" + alert.getType() + "] " + alert.getMessage() +
                         " | Physician Justification: \"" + justification + "\"";
        auditLogDAO.save(new AuditLog(doctorUsername, "CRITICAL_OVERRIDE", "PrescriptionAlert",
                String.valueOf(alert.getId()), details));
        logger.warn("AUDIT RECORD: Dr. {} performed a CRITICAL OVERRIDE: {}", doctorUsername, justification);
    }

    public void logPrescriptionSaved(String doctorUsername, Long prescriptionId, BigDecimal totalCost) {
        String details = "Prescription approved and finalized. Total: ₹" + totalCost;
        auditLogDAO.save(new AuditLog(doctorUsername, "PRESCRIPTION_APPROVED", "Prescription",
                String.valueOf(prescriptionId), details));
    }

    public void logStaffAction(String operator, String targetUser, String action, String details) {
        auditLogDAO.save(new AuditLog(operator, action, "Account", targetUser, details));
    }

    public void logRuleAction(String operator, String action, String ruleDesc) {
        auditLogDAO.save(new AuditLog(operator, action, "InteractionRule", null, ruleDesc));
    }

    public void logPatientAction(String operator, String action, String patientId, String details) {
        auditLogDAO.save(new AuditLog(operator, action, "Patient", patientId, details));
    }

    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogDAO.findRecent(limit);
    }

    public List<AuditLog> searchLogs(String operator, String operationType, LocalDate startDate, LocalDate endDate, int limit) {
        return auditLogDAO.search(operator, operationType, startDate, endDate, limit);
    }
}
