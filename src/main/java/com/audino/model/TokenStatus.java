package com.audino.model;

/**
 * State machine representation for daily outpatient queue tokens.
 * Enforces legal state transitions along a directed clinical workflow graph.
 */
public enum TokenStatus {
    WAITING("Waiting in Queue"),
    IN_CONSULTATION("In Consultation"),
    COMPLETED("Consultation Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    TokenStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Evaluates whether a transition from this state to the target state is valid.
     * Legal transitions:
     * - WAITING -> IN_CONSULTATION
     * - WAITING -> CANCELLED
     * - IN_CONSULTATION -> COMPLETED
     * All other transitions are strictly illegal.
     *
     * @param target Target state
     * @return true if legal transition, false otherwise
     */
    public boolean canTransitionTo(TokenStatus target) {
        if (target == null) return false;
        switch (this) {
            case WAITING:
                return target == IN_CONSULTATION || target == CANCELLED;
            case IN_CONSULTATION:
                return target == COMPLETED;
            case COMPLETED:
            case CANCELLED:
            default:
                return false; // Terminal states
        }
    }
}
