package com.audino.service;

import com.audino.dao.AuditLogDAO;
import com.audino.dao.PatientDAO;
import com.audino.dao.TokenDAO;
import com.audino.exception.InvalidTokenStateException;
import com.audino.model.AuditLog;
import com.audino.model.Patient;
import com.audino.model.Token;
import com.audino.model.TokenStatus;
import com.audino.observer.QueueObserver;
import com.audino.observer.QueueSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service managing outpatient queue workflows and acting as the Subject in the Observer pattern.
 * Synchronizes queue state in-process and cross-instance via background polling.
 *
 * // OOM: Observer Pattern – QueueService acts as the Subject broadcasting queue state mutations to registered Observers.
 */
public class QueueService implements QueueSubject {

    private static final Logger logger = LoggerFactory.getLogger(QueueService.class);

    private final TokenDAO tokenDAO;
    private final PatientDAO patientDAO;
    private final AuditLogDAO auditLogDAO;

    // Thread-safe list of observers
    private final List<QueueObserver> observers = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService pollingExecutor;
    private String lastQueueFingerprint = "";

    public QueueService(TokenDAO tokenDAO, PatientDAO patientDAO, AuditLogDAO auditLogDAO) {
        this.tokenDAO = Objects.requireNonNull(tokenDAO, "TokenDAO must not be null");
        this.patientDAO = Objects.requireNonNull(patientDAO, "PatientDAO must not be null");
        this.auditLogDAO = Objects.requireNonNull(auditLogDAO, "AuditLogDAO must not be null");
    }

    /**
     * Starts the background queue polling daemon to synchronize multi-instance clinic deployments.
     *
     * @param intervalSeconds Polling cadence in seconds (typically 3s)
     */
    public synchronized void startPolling(int intervalSeconds) {
        if (pollingExecutor == null || pollingExecutor.isShutdown()) {
            pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Queue-Polling-Daemon");
                t.setDaemon(true);
                return t;
            });

            int period = intervalSeconds > 0 ? intervalSeconds : 3;
            pollingExecutor.scheduleAtFixedRate(this::pollQueueForChanges, period, period, TimeUnit.SECONDS);
            logger.info("Started QueueService background polling daemon (every {}s).", period);
        }
    }

    public synchronized void stopPolling() {
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            pollingExecutor.shutdownNow();
            logger.info("Stopped QueueService background polling daemon.");
        }
    }

    private void pollQueueForChanges() {
        try {
            List<Token> current = tokenDAO.findTodayTokens();
            String currentFingerprint = computeQueueFingerprint(current);

            if (!currentFingerprint.equals(lastQueueFingerprint)) {
                lastQueueFingerprint = currentFingerprint;
                logger.debug("Cross-instance queue state change detected via polling. Broadcasting to observers...");
                broadcast(current);
            }
        } catch (Exception e) {
            logger.warn("Queue polling check failed: {}", e.getMessage());
        }
    }

    private String computeQueueFingerprint(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            sb.append(t.getId()).append(":").append(t.getStatus()).append(";");
        }
        return sb.toString();
    }

    @Override
    public void registerObserver(QueueObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            logger.debug("Registered new QueueObserver: {}", observer.getClass().getSimpleName());
            // Immediately notify new observer of current state
            observer.onQueueUpdated(getTodayTokensWithPatients());
        }
    }

    @Override
    public void removeObserver(QueueObserver observer) {
        if (observer != null) {
            observers.remove(observer);
            logger.debug("Unregistered QueueObserver: {}", observer.getClass().getSimpleName());
        }
    }

    @Override
    public void notifyObservers() {
        List<Token> tokens = getTodayTokensWithPatients();
        lastQueueFingerprint = computeQueueFingerprint(tokens);
        broadcast(tokens);
    }

    private void broadcast(List<Token> tokens) {
        for (QueueObserver observer : observers) {
            try {
                observer.onQueueUpdated(tokens);
            } catch (Exception e) {
                logger.error("Error notifying QueueObserver {}: {}", observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * Issues a new outpatient token for today's clinic session.
     */
    public Token issueToken(Long patientId, String doctorUsername, String issuedBy) {
        LocalDate today = LocalDate.now();
        String nextNo = tokenDAO.getNextTokenNo(today);
        Token token = new Token(nextNo, patientId, doctorUsername);
        token = tokenDAO.save(token);

        auditLogDAO.save(new AuditLog(
            issuedBy != null ? issuedBy : "SYSTEM",
            "TOKEN_ISSUED",
            "Token",
            token.getTokenNo(),
            "Issued token " + token.getTokenNo() + " for patient ID " + patientId + " (Assigned to: " + doctorUsername + ")"
        ));

        logger.info("Issued token {} for patient ID {} (assigned: {})", token.getTokenNo(), patientId, doctorUsername);
        notifyObservers();
        return token;
    }

    /**
     * Advances the oldest waiting token assigned to the doctor into consultation.
     */
    public Optional<Token> callNextPatient(String doctorUsername) throws InvalidTokenStateException {
        LocalDate today = LocalDate.now();
        Optional<Token> nextOpt = tokenDAO.findNextWaitingToken(doctorUsername, today);

        if (nextOpt.isPresent()) {
            Token token = nextOpt.get();
            token.transitionTo(TokenStatus.IN_CONSULTATION);
            token.setDoctorUsername(doctorUsername);
            tokenDAO.updateStatus(token.getId(), TokenStatus.IN_CONSULTATION);

            auditLogDAO.save(new AuditLog(
                doctorUsername,
                "TOKEN_CALLED",
                "Token",
                token.getTokenNo(),
                "Doctor " + doctorUsername + " called token " + token.getTokenNo() + " into consultation"
            ));

            logger.info("Doctor {} called token {} into consultation.", doctorUsername, token.getTokenNo());
            notifyObservers();
            return Optional.of(token);
        }
        return Optional.empty();
    }

    /**
     * Completes an active consultation.
     */
    public void completeConsultation(Long tokenId, String doctorUsername) throws InvalidTokenStateException {
        Optional<Token> tokenOpt = tokenDAO.findById(tokenId);
        if (tokenOpt.isPresent()) {
            Token token = tokenOpt.get();
            token.transitionTo(TokenStatus.COMPLETED);
            tokenDAO.updateStatus(token.getId(), TokenStatus.COMPLETED);

            auditLogDAO.save(new AuditLog(
                doctorUsername != null ? doctorUsername : token.getDoctorUsername(),
                "TOKEN_COMPLETED",
                "Token",
                token.getTokenNo(),
                "Consultation completed for token " + token.getTokenNo()
            ));

            logger.info("Completed consultation for token {}.", token.getTokenNo());
            notifyObservers();
        }
    }

    /**
     * Cancels an outpatient token.
     */
    public void cancelToken(Long tokenId, String cancelledBy, String reason) throws InvalidTokenStateException {
        Optional<Token> tokenOpt = tokenDAO.findById(tokenId);
        if (tokenOpt.isPresent()) {
            Token token = tokenOpt.get();
            token.transitionTo(TokenStatus.CANCELLED);
            tokenDAO.updateStatus(token.getId(), TokenStatus.CANCELLED);

            auditLogDAO.save(new AuditLog(
                cancelledBy,
                "TOKEN_CANCELLED",
                "Token",
                token.getTokenNo(),
                "Cancelled token " + token.getTokenNo() + " (Reason: " + reason + ")"
            ));

            logger.info("Cancelled token {} by {}.", token.getTokenNo(), cancelledBy);
            notifyObservers();
        }
    }

    /**
     * Retrieves today's tokens with hydrated Patient domain references.
     */
    public List<Token> getTodayTokensWithPatients() {
        List<Token> tokens = tokenDAO.findTodayTokens();
        for (Token t : tokens) {
            patientDAO.findById(t.getPatientId()).ifPresent(t::setPatient);
        }
        return tokens;
    }

    public List<QueueObserver> getObservers() {
        return Collections.unmodifiableList(observers);
    }
}
