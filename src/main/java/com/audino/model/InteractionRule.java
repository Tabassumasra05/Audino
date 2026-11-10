package com.audino.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a clinical drug interaction or contraindication rule.
 */
public class InteractionRule {

    private Long id;
    private final AlertType type;
    private final String keyword1;
    private final String keyword2;
    private final AlertLevel severity;
    private final String description;
    private final String recommendation;
    private final LocalDateTime createdAt;

    public InteractionRule(Long id, AlertType type, String keyword1, String keyword2,
                           AlertLevel severity, String description, String recommendation,
                           LocalDateTime createdAt) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "Interaction type must not be null");
        this.keyword1 = Objects.requireNonNull(keyword1, "Keyword 1 must not be null").trim();
        this.keyword2 = Objects.requireNonNull(keyword2, "Keyword 2 must not be null").trim();
        this.severity = Objects.requireNonNull(severity, "Severity level must not be null");
        this.description = Objects.requireNonNull(description, "Description must not be null");
        this.recommendation = recommendation;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public InteractionRule(Long id, AlertType type, String keyword1, String keyword2,
                           AlertLevel severity, String description, String recommendation) {
        this(id, type, keyword1, keyword2, severity, description, recommendation, LocalDateTime.now());
    }

    public InteractionRule(AlertType type, String keyword1, String keyword2,
                           AlertLevel severity, String description, String recommendation) {
        this(null, type, keyword1, keyword2, severity, description, recommendation, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AlertType getType() {
        return type;
    }

    public String getKeyword1() {
        return keyword1;
    }

    public String getKeyword2() {
        return keyword2;
    }

    public AlertLevel getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InteractionRule that)) return false;
        return type == that.type &&
               Objects.equals(keyword1.toLowerCase(), that.keyword1.toLowerCase()) &&
               Objects.equals(keyword2.toLowerCase(), that.keyword2.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, keyword1.toLowerCase(), keyword2.toLowerCase());
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + keyword1 + " + " + keyword2 + " (" + type + ")";
    }
}
