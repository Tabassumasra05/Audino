package com.audino.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable forensic audit log record for tracking system, security, and clinical override events.
 */
public class AuditLog {

    private Long id;
    private final LocalDateTime timestamp;
    private final String operatorUsername;
    private final String operationType;
    private final String entityType;
    private final String entityId;
    private final String details;

    public AuditLog(Long id, LocalDateTime timestamp, String operatorUsername,
                    String operationType, String entityType, String entityId, String details) {
        this.id = id;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.operatorUsername = Objects.requireNonNull(operatorUsername, "Operator username must not be null");
        this.operationType = Objects.requireNonNull(operationType, "Operation type must not be null");
        this.entityType = Objects.requireNonNull(entityType, "Entity type must not be null");
        this.entityId = entityId;
        this.details = details;
    }

    public AuditLog(String operatorUsername, String operationType, String entityType, String entityId, String details) {
        this(null, LocalDateTime.now(), operatorUsername, operationType, entityType, entityId, details);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + operatorUsername + " performed " + operationType + " on " + entityType + " (" + entityId + "): " + details;
    }
}
