package com.audino;

import com.audino.dao.AuditLogDAO;
import com.audino.dao.PatientDAO;
import com.audino.dao.TokenDAO;
import com.audino.model.AuditLog;
import com.audino.model.Gender;
import com.audino.model.Patient;
import com.audino.model.Token;
import com.audino.model.TokenStatus;
import com.audino.observer.QueueObserver;
import com.audino.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Test: Observer Pattern & QueueService")
class QueueServiceObserverTest {

    private QueueService queueService;
    private MockTokenDAO tokenDAO;
    private MockPatientDAO patientDAO;
    private MockAuditLogDAO auditLogDAO;

    @BeforeEach
    void setUp() {
        tokenDAO = new MockTokenDAO();
        patientDAO = new MockPatientDAO();
        auditLogDAO = new MockAuditLogDAO();

        patientDAO.save(new Patient(1L, "P-1001", "Ramesh", "Sharma", LocalDate.of(1980, 1, 1), Gender.MALE, "9820112345", "Mumbai", List.of(), List.of(), null));

        queueService = new QueueService(tokenDAO, patientDAO, auditLogDAO);
    }

    @Test
    @DisplayName("Registered observers are notified when a new token is issued")
    void testObserverNotifiedOnTokenIssue() {
        FakeQueueObserver observer = new FakeQueueObserver();
        queueService.registerObserver(observer);

        // Initial snapshot upon registration
        int initialCount = observer.updateCallCount;

        // Issue new token
        Token issued = queueService.issueToken(1L, "doctor", "reception");

        assertThat(observer.updateCallCount).isEqualTo(initialCount + 1);
        assertThat(observer.lastReceivedTokens).anyMatch(t -> t.getTokenNo().equals(issued.getTokenNo()));
    }

    @Test
    @DisplayName("Registered observers are notified when a token status changes (call next / complete)")
    void testObserverNotifiedOnStatusChange() throws Exception {
        FakeQueueObserver observer = new FakeQueueObserver();
        queueService.registerObserver(observer);

        Token t = queueService.issueToken(1L, "doctor", "reception");
        int countBefore = observer.updateCallCount;

        queueService.callNextPatient("doctor");

        assertThat(observer.updateCallCount).isGreaterThan(countBefore);
        assertThat(observer.lastReceivedTokens).anyMatch(tok -> tok.getId().equals(t.getId()) && tok.getStatus() == TokenStatus.IN_CONSULTATION);
    }

    @Test
    @DisplayName("Unregistered observers do NOT receive further updates after deregistration")
    void testUnregisteredObserverIsNotNotified() {
        FakeQueueObserver activeObserver = new FakeQueueObserver();
        FakeQueueObserver removedObserver = new FakeQueueObserver();

        queueService.registerObserver(activeObserver);
        queueService.registerObserver(removedObserver);

        // Deregister one observer
        queueService.removeObserver(removedObserver);
        int callCountAtRemoval = removedObserver.updateCallCount;

        // Issue token
        queueService.issueToken(1L, "doctor", "reception");

        // Active observer updated, removed observer NOT updated
        assertThat(activeObserver.updateCallCount).isGreaterThan(callCountAtRemoval);
        assertThat(removedObserver.updateCallCount).isEqualTo(callCountAtRemoval);
    }

    private static class FakeQueueObserver implements QueueObserver {
        int updateCallCount = 0;
        List<Token> lastReceivedTokens = new ArrayList<>();

        @Override
        public void onQueueUpdated(List<Token> tokens) {
            updateCallCount++;
            lastReceivedTokens = new ArrayList<>(tokens);
        }
    }

    private static class MockTokenDAO implements TokenDAO {
        private final List<Token> tokens = new ArrayList<>();
        private long idSeq = 1;

        @Override public Optional<Token> findById(Long id) { return tokens.stream().filter(t -> t.getId().equals(id)).findFirst(); }
        @Override public Optional<Token> findByTokenNoAndDate(String no, LocalDate d) { return tokens.stream().filter(t -> t.getTokenNo().equals(no)).findFirst(); }
        @Override public List<Token> findTodayTokens() { return new ArrayList<>(tokens); }
        @Override public List<Token> findTodayTokensByDoctor(String doc) { return new ArrayList<>(tokens); }
        @Override public Optional<Token> findNextWaitingToken(String doc, LocalDate d) {
            return tokens.stream().filter(t -> t.getStatus() == TokenStatus.WAITING).findFirst();
        }
        @Override
        public Token save(Token token) {
            token.setId(idSeq++);
            tokens.add(token);
            return token;
        }
        @Override
        public void updateStatus(Long tokenId, TokenStatus newStatus) {
            findById(tokenId).ifPresent(t -> t.setStatus(newStatus));
        }
        @Override public void assignDoctor(Long id, String doc) {}
        @Override public String getNextTokenNo(LocalDate date) { return String.format("T-%03d", tokens.size() + 1); }
        @Override public int countTodayTokensByStatus(TokenStatus status) { return (int) tokens.stream().filter(t -> t.getStatus() == status).count(); }
    }

    private static class MockPatientDAO implements PatientDAO {
        private final List<Patient> patients = new ArrayList<>();
        @Override public Optional<Patient> findById(Long id) { return patients.stream().filter(p -> p.getId().equals(id)).findFirst(); }
        @Override public Optional<Patient> findByPatientId(String pid) { return Optional.empty(); }
        @Override public List<Patient> findAll() { return patients; }
        @Override public List<Patient> search(String q) { return patients; }
        @Override public Patient save(Patient p) { patients.add(p); return p; }
        @Override public void update(Patient p) {}
        @Override public List findAllergiesByPatientId(Long id) { return List.of(); }
        @Override public List findConditionsByPatientId(Long id) { return List.of(); }
        @Override public boolean existsByPhoneAndName(String ph, String f, String l) { return false; }
        @Override public String getNextPatientId() { return "P-1"; }
    }

    private static class MockAuditLogDAO implements AuditLogDAO {
        @Override public AuditLog save(AuditLog log) { return log; }
        @Override public List<AuditLog> findRecent(int limit) { return List.of(); }
        @Override public List<AuditLog> search(String op, String type, LocalDate s, LocalDate e, int l) { return List.of(); }
    }
}
