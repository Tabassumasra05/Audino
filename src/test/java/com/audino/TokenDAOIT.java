package com.audino;

import com.audino.dao.PatientDAO;
import com.audino.dao.TokenDAO;
import com.audino.dao.jdbc.PatientDAOJdbc;
import com.audino.dao.jdbc.TokenDAOJdbc;
import com.audino.model.Gender;
import com.audino.model.Patient;
import com.audino.model.Token;
import com.audino.model.TokenStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration Test: TokenDAO Native JDBC Queue Operations")
class TokenDAOIT extends AbstractDAOIntegrationTest {

    private TokenDAO tokenDAO;
    private Patient testPatient;

    @BeforeEach
    void setUp() {
        tokenDAO = new TokenDAOJdbc(dataSource);
        PatientDAO patientDAO = new PatientDAOJdbc(dataSource);

        // Ensure we have a persistent patient to reference
        String phone = "94" + (System.currentTimeMillis() % 100000000L);
        testPatient = patientDAO.save(new Patient(
                null,
                patientDAO.getNextPatientId(),
                "TokenTest",
                "Patient",
                LocalDate.of(1985, 3, 10),
                Gender.FEMALE,
                phone,
                "Hyderabad"
        ));
    }

    @Test
    @DisplayName("getNextTokenNo() yields sequential token numbers T-001, T-002, etc.")
    void testNextTokenSequence() {
        String tokenNo1 = tokenDAO.getNextTokenNo(LocalDate.now());
        assertThat(tokenNo1).matches("T-\\d{3}");

        Token t1 = new Token(null, tokenNo1, LocalDate.now(), testPatient.getId(), "doctor", TokenStatus.WAITING);
        tokenDAO.save(t1);

        String tokenNo2 = tokenDAO.getNextTokenNo(LocalDate.now());
        assertThat(tokenNo2).matches("T-\\d{3}");

        int num1 = Integer.parseInt(tokenNo1.substring(2));
        int num2 = Integer.parseInt(tokenNo2.substring(2));
        assertThat(num2).isEqualTo(num1 + 1);
    }

    @Test
    @DisplayName("updateStatus() transitions token from WAITING -> IN_CONSULTATION -> COMPLETED")
    void testTokenLifecycleStatusTransitions() {
        String tokenNo = tokenDAO.getNextTokenNo(LocalDate.now());
        Token token = new Token(null, tokenNo, LocalDate.now(), testPatient.getId(), "doctor", TokenStatus.WAITING);
        Token saved = tokenDAO.save(token);

        assertThat(saved.getStatus()).isEqualTo(TokenStatus.WAITING);

        // Transition 1: WAITING -> IN_CONSULTATION
        tokenDAO.updateStatus(saved.getId(), TokenStatus.IN_CONSULTATION);
        Token inConsult = tokenDAO.findById(saved.getId()).orElseThrow();
        assertThat(inConsult.getStatus()).isEqualTo(TokenStatus.IN_CONSULTATION);
        assertThat(inConsult.getCalledAt()).isNotNull();

        // Transition 2: IN_CONSULTATION -> COMPLETED
        tokenDAO.updateStatus(saved.getId(), TokenStatus.COMPLETED);
        Token completed = tokenDAO.findById(saved.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(TokenStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("findTodayTokensByDoctor() filters tokens assigned to specific doctor or unassigned")
    void testDoctorQueueFiltering() {
        String tNo1 = tokenDAO.getNextTokenNo(LocalDate.now());
        tokenDAO.save(new Token(null, tNo1, LocalDate.now(), testPatient.getId(), "doctor", TokenStatus.WAITING));

        String tNo2 = tokenDAO.getNextTokenNo(LocalDate.now());
        tokenDAO.save(new Token(null, tNo2, LocalDate.now(), testPatient.getId(), "doctor2", TokenStatus.WAITING));

        List<Token> doctorQueue = tokenDAO.findTodayTokensByDoctor("doctor");
        assertThat(doctorQueue).anyMatch(t -> tNo1.equals(t.getTokenNo()));
        assertThat(doctorQueue).noneMatch(t -> tNo2.equals(t.getTokenNo()));
    }
}
