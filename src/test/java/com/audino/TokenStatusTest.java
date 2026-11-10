package com.audino;

import com.audino.exception.InvalidTokenStateException;
import com.audino.model.Token;
import com.audino.model.TokenStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit Test: Token State Machine & Transitions")
class TokenStatusTest {

    @Test
    @DisplayName("Legal: WAITING -> IN_CONSULTATION advances state and sets called_at")
    void testWaitingToInConsultation() throws Exception {
        Token token = new Token("T-001", 101L, "doctor");
        assertThat(token.getStatus()).isEqualTo(TokenStatus.WAITING);

        token.transitionTo(TokenStatus.IN_CONSULTATION);

        assertThat(token.getStatus()).isEqualTo(TokenStatus.IN_CONSULTATION);
        assertThat(token.getCalledAt()).isNotNull();
        assertThat(token.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("Legal: IN_CONSULTATION -> COMPLETED finishes consultation and sets completed_at")
    void testInConsultationToCompleted() throws Exception {
        Token token = new Token("T-002", 102L, "doctor");
        token.transitionTo(TokenStatus.IN_CONSULTATION);

        token.transitionTo(TokenStatus.COMPLETED);

        assertThat(token.getStatus()).isEqualTo(TokenStatus.COMPLETED);
        assertThat(token.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Legal: WAITING -> CANCELLED cancels token")
    void testWaitingToCancelled() throws Exception {
        Token token = new Token("T-003", 103L, "doctor");
        token.transitionTo(TokenStatus.CANCELLED);

        assertThat(token.getStatus()).isEqualTo(TokenStatus.CANCELLED);
    }

    @Test
    @DisplayName("Illegal: WAITING -> COMPLETED directly bypasses consultation and throws InvalidTokenStateException")
    void testWaitingToCompletedBypassThrows() {
        Token token = new Token("T-004", 104L, "doctor");

        assertThatThrownBy(() -> token.transitionTo(TokenStatus.COMPLETED))
                .isInstanceOf(InvalidTokenStateException.class)
                .hasMessageContaining("Illegal token transition");
    }

    @Test
    @DisplayName("Illegal: IN_CONSULTATION -> WAITING reverses queue order and throws InvalidTokenStateException")
    void testInConsultationToWaitingThrows() throws Exception {
        Token token = new Token("T-005", 105L, "doctor");
        token.transitionTo(TokenStatus.IN_CONSULTATION);

        assertThatThrownBy(() -> token.transitionTo(TokenStatus.WAITING))
                .isInstanceOf(InvalidTokenStateException.class)
                .hasMessageContaining("Illegal token transition");
    }

    @Test
    @DisplayName("Illegal: COMPLETED is terminal; cannot transition to any other status")
    void testCompletedIsTerminal() throws Exception {
        Token token = new Token("T-006", 106L, "doctor");
        token.transitionTo(TokenStatus.IN_CONSULTATION);
        token.transitionTo(TokenStatus.COMPLETED);

        assertThatThrownBy(() -> token.transitionTo(TokenStatus.WAITING))
                .isInstanceOf(InvalidTokenStateException.class);
        assertThatThrownBy(() -> token.transitionTo(TokenStatus.IN_CONSULTATION))
                .isInstanceOf(InvalidTokenStateException.class);
        assertThatThrownBy(() -> token.transitionTo(TokenStatus.CANCELLED))
                .isInstanceOf(InvalidTokenStateException.class);
    }

    @Test
    @DisplayName("Illegal: CANCELLED is terminal; cannot transition to any other status")
    void testCancelledIsTerminal() throws Exception {
        Token token = new Token("T-007", 107L, "doctor");
        token.transitionTo(TokenStatus.CANCELLED);

        assertThatThrownBy(() -> token.transitionTo(TokenStatus.WAITING))
                .isInstanceOf(InvalidTokenStateException.class);
        assertThatThrownBy(() -> token.transitionTo(TokenStatus.IN_CONSULTATION))
                .isInstanceOf(InvalidTokenStateException.class);
    }
}
