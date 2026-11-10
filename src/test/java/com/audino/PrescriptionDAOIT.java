package com.audino;

import com.audino.dao.MedicationDAO;
import com.audino.dao.PatientDAO;
import com.audino.dao.PrescriptionDAO;
import com.audino.dao.TokenDAO;
import com.audino.dao.jdbc.MedicationDAOJdbc;
import com.audino.dao.jdbc.PatientDAOJdbc;
import com.audino.dao.jdbc.PrescriptionDAOJdbc;
import com.audino.dao.jdbc.TokenDAOJdbc;
import com.audino.exception.DataAccessException;
import com.audino.model.AlertLevel;
import com.audino.model.AlertType;
import com.audino.model.Frequency;
import com.audino.model.Gender;
import com.audino.model.Medication;
import com.audino.model.Patient;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionItem;
import com.audino.model.PrescriptionStatus;
import com.audino.model.Token;
import com.audino.model.TokenStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Integration Test: PrescriptionDAO Transactional Integrity & Composition")
class PrescriptionDAOIT extends AbstractDAOIntegrationTest {

    private PrescriptionDAO prescriptionDAO;
    private MedicationDAO medicationDAO;
    private Patient testPatient;
    private Token testToken;
    private Medication testMed;

    @BeforeEach
    void setUp() {
        medicationDAO = new MedicationDAOJdbc(dataSource);
        prescriptionDAO = new PrescriptionDAOJdbc(dataSource, medicationDAO);
        PatientDAO patientDAO = new PatientDAOJdbc(dataSource);
        TokenDAO tokenDAO = new TokenDAOJdbc(dataSource);

        // Prepare test patient
        String phone = "93" + (System.currentTimeMillis() % 100000000L);
        testPatient = patientDAO.save(new Patient(
                null,
                patientDAO.getNextPatientId(),
                "Prescription",
                "Patient",
                LocalDate.of(1975, 4, 15),
                Gender.MALE,
                phone,
                "Mumbai"
        ));

        // Prepare test token
        String tokenNo = tokenDAO.getNextTokenNo(LocalDate.now());
        testToken = tokenDAO.save(new Token(null, tokenNo, LocalDate.now(), testPatient.getId(), "doctor", TokenStatus.IN_CONSULTATION));

        // Retrieve a seeded medication (e.g. Paracetamol or first available)
        List<Medication> meds = medicationDAO.findAll();
        assertThat(meds).isNotEmpty();
        testMed = meds.get(0);
    }

    @Test
    @DisplayName("saveWithTransaction() atomically persists prescription, items, and override alerts")
    void testSaveWithTransactionComplete() {
        Prescription rx = new Prescription(
                null,
                testPatient.getId(),
                "doctor",
                testToken.getId(),
                PrescriptionStatus.APPROVED,
                new BigDecimal("150.00"),
                LocalDateTime.now()
        );

        PrescriptionItem item = new PrescriptionItem(
                null,
                null,
                testMed.getId(),
                "1 tablet",
                Frequency.THRICE_DAILY,
                5,
                "After meals with warm water",
                testMed.getPricePerUnit(),
                15,
                testMed.getPricePerUnit().multiply(BigDecimal.valueOf(15))
        );
        rx.addItem(item);

        PrescriptionAlert alert = new PrescriptionAlert(
                null,
                null,
                AlertLevel.WARNING,
                AlertType.DRUG_DRUG,
                "Potential mild interaction flagged",
                true,
                "Verified patient tolerates combination without complications",
                "doctor",
                LocalDateTime.now()
        );
        rx.addAlert(alert);

        Prescription saved = prescriptionDAO.saveWithTransaction(rx);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isPositive();
        assertThat(saved.getItems().get(0).getId()).isNotNull();
        assertThat(saved.getAlerts().get(0).getId()).isNotNull();

        // Query back by ID
        Optional<Prescription> fetchedOpt = prescriptionDAO.findById(saved.getId());
        assertThat(fetchedOpt).isPresent();

        Prescription fetched = fetchedOpt.get();
        assertThat(fetched.getPatientId()).isEqualTo(testPatient.getId());
        assertThat(fetched.getDoctorUsername()).isEqualTo("doctor");
        assertThat(fetched.getStatus()).isEqualTo(PrescriptionStatus.APPROVED);

        // Composition check: items and alerts populated
        assertThat(fetched.getItems()).hasSize(1);
        PrescriptionItem fetchedItem = fetched.getItems().get(0);
        assertThat(fetchedItem.getMedicationId()).isEqualTo(testMed.getId());
        assertThat(fetchedItem.getMedication()).isNotNull();
        assertThat(fetchedItem.getInstructions()).isEqualTo("After meals with warm water");

        assertThat(fetched.getAlerts()).hasSize(1);
        PrescriptionAlert fetchedAlert = fetched.getAlerts().get(0);
        assertThat(fetchedAlert.getLevel()).isEqualTo(AlertLevel.WARNING);
        assertThat(fetchedAlert.isAcknowledged()).isTrue();
        assertThat(fetchedAlert.getOverrideNote()).contains("tolerates combination");
    }

    @Test
    @DisplayName("saveWithTransaction() rolls back completely when foreign key constraint is violated")
    void testTransactionRollbackOnInvalidData() {
        int initialCount = prescriptionDAO.countTodayPrescriptions();

        // Invalid patient ID (-9999L) will trigger foreign key failure
        Prescription invalidRx = new Prescription(
                null,
                -9999L,
                "doctor",
                testToken.getId(),
                PrescriptionStatus.APPROVED,
                new BigDecimal("50.00"),
                LocalDateTime.now()
        );

        assertThatThrownBy(() -> prescriptionDAO.saveWithTransaction(invalidRx))
                .isInstanceOf(DataAccessException.class);

        // Ensure database state rolled back cleanly
        assertThat(prescriptionDAO.countTodayPrescriptions()).isEqualTo(initialCount);
    }
}
