package com.audino.model;

import com.audino.exception.InvalidTokenStateException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Outpatient queue token representing a scheduled consultation slot for today.
 *
 * // OOM: Association & State Machine – associates Patient with Doctor and enforces strict lifecycle state transitions.
 */
public class Token {

    private Long id;
    private final String tokenNo;
    private final LocalDate tokenDate;
    private final Long patientId;
    private String doctorUsername;
    private TokenStatus status;
    private final LocalDateTime issuedAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;

    // Transient navigational references for UI presentation
    private Patient patient;
    private Doctor doctor;

    public Token(Long id, String tokenNo, LocalDate tokenDate, Long patientId,
                 String doctorUsername, TokenStatus status,
                 LocalDateTime issuedAt, LocalDateTime calledAt, LocalDateTime completedAt) {
        this.id = id;
        this.tokenNo = Objects.requireNonNull(tokenNo, "Token number must not be null");
        this.tokenDate = tokenDate != null ? tokenDate : LocalDate.now();
        this.patientId = Objects.requireNonNull(patientId, "Patient ID must not be null");
        this.doctorUsername = doctorUsername;
        this.status = status != null ? status : TokenStatus.WAITING;
        this.issuedAt = issuedAt != null ? issuedAt : LocalDateTime.now();
        this.calledAt = calledAt;
        this.completedAt = completedAt;
    }

    public Token(Long id, String tokenNo, LocalDate tokenDate, Long patientId,
                 String doctorUsername, TokenStatus status) {
        this(id, tokenNo, tokenDate, patientId, doctorUsername, status,
             LocalDateTime.now(), null, null);
    }

    public Token(String tokenNo, Long patientId, String doctorUsername) {
        this(null, tokenNo, LocalDate.now(), patientId, doctorUsername, TokenStatus.WAITING,
             LocalDateTime.now(), null, null);
    }

    /**
     * Executes state machine transition along the legal clinical directed graph.
     * Throws InvalidTokenStateException if the requested transition is illegal.
     *
     * @param target Desired target state
     * @throws InvalidTokenStateException if transition violates state invariants
     */
    public void transitionTo(TokenStatus target) throws InvalidTokenStateException {
        if (!this.status.canTransitionTo(target)) {
            throw new InvalidTokenStateException(
                "Illegal token transition: cannot move token '" + tokenNo + "' from " + status + " to " + target
            );
        }

        this.status = target;
        if (target == TokenStatus.IN_CONSULTATION && this.calledAt == null) {
            this.calledAt = LocalDateTime.now();
        } else if (target == TokenStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenNo() {
        return tokenNo;
    }

    public LocalDate getTokenDate() {
        return tokenDate;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getDoctorUsername() {
        return doctorUsername;
    }

    public void setDoctorUsername(String doctorUsername) {
        this.doctorUsername = doctorUsername;
    }

    public TokenStatus getStatus() {
        return status;
    }

    public void setStatus(TokenStatus status) {
        this.status = status;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token token)) return false;
        return Objects.equals(tokenNo, token.tokenNo) && Objects.equals(tokenDate, token.tokenDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenNo, tokenDate);
    }

    @Override
    public String toString() {
        return tokenNo + " [" + status + "]";
    }
}
