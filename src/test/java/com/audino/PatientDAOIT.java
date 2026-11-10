package com.audino;

import com.audino.dao.PatientDAO;
import com.audino.dao.jdbc.PatientDAOJdbc;
import com.audino.model.Gender;
import com.audino.model.Patient;
import com.audino.model.PatientAllergy;
import com.audino.model.PatientCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration Test: PatientDAO Native JDBC Operations & Composition")
class PatientDAOIT extends AbstractDAOIntegrationTest {

    private PatientDAO patientDAO;

    @BeforeEach
    void setUp() {
        patientDAO = new PatientDAOJdbc(dataSource);
    }

    @Test
    @DisplayName("getNextPatientId() produces valid P-XXXX sequential identifier")
    void testGeneratePatientId() {
        String nextId = patientDAO.getNextPatientId();
        assertThat(nextId).isNotNull();
        assertThat(nextId).matches("P-\\d+");
    }

    @Test
    @DisplayName("save() persists patient with composition allergies and chronic conditions")
    void testSavePatientWithComposition() {
        String patientId = patientDAO.getNextPatientId();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6);
        String phone = "98" + (System.currentTimeMillis() % 100000000L);

        Patient patient = new Patient(
                null,
                patientId,
                "Ramesh",
                "Patel_" + randomSuffix,
                LocalDate.of(1980, 5, 12),
                Gender.MALE,
                phone,
                "123 MG Road, Bengaluru"
        );

        patient.addAllergy(new PatientAllergy("Penicillin", "CRITICAL"));
        patient.addAllergy(new PatientAllergy("Sulfa", "WARNING"));
        patient.addCondition(new PatientCondition("Hypertension", LocalDate.of(2018, 1, 1)));
        patient.addCondition(new PatientCondition("Type 2 Diabetes", LocalDate.of(2020, 5, 1)));

        Patient saved = patientDAO.save(patient);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isPositive();
        assertThat(saved.getAllergies()).hasSize(2);
        assertThat(saved.getConditions()).hasSize(2);

        // Fetch back and assert composition lists
        Optional<Patient> retrievedOpt = patientDAO.findById(saved.getId());
        assertThat(retrievedOpt).isPresent();

        Patient retrieved = retrievedOpt.get();
        assertThat(retrieved.getPatientId()).isEqualTo(patientId);
        assertThat(retrieved.getFirstName()).isEqualTo("Ramesh");
        assertThat(retrieved.getAllergies())
                .hasSize(2)
                .extracting(PatientAllergy::getAllergen)
                .containsExactlyInAnyOrder("Penicillin", "Sulfa");

        assertThat(retrieved.getConditions())
                .hasSize(2)
                .extracting(PatientCondition::getConditionName)
                .containsExactlyInAnyOrder("Hypertension", "Type 2 Diabetes");
    }

    @Test
    @DisplayName("search() finds patients by partial ID, name, or phone number")
    void testSearchPatients() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 5);
        String phone = "99" + (System.currentTimeMillis() % 100000000L);
        String patientId = patientDAO.getNextPatientId();

        Patient p = new Patient(null, patientId, "Devika", "Nair_" + uniqueSuffix,
                LocalDate.of(1992, 8, 20), Gender.FEMALE, phone, "Kochi");
        patientDAO.save(p);

        // Search by last name suffix
        List<Patient> byName = patientDAO.search(uniqueSuffix);
        assertThat(byName).isNotEmpty();
        assertThat(byName.get(0).getLastName()).contains(uniqueSuffix);

        // Search by phone
        List<Patient> byPhone = patientDAO.search(phone.substring(3, 8));
        assertThat(byPhone).isNotEmpty();
        assertThat(byPhone).anyMatch(pat -> phone.equals(pat.getPhone()));

        // Search by patient ID
        List<Patient> byId = patientDAO.search(patientId);
        assertThat(byId).hasSize(1);
        assertThat(byId.get(0).getPatientId()).isEqualTo(patientId);
    }

    @Test
    @DisplayName("existsByPhoneAndName() correctly detects registered contact numbers and names")
    void testExistsByPhoneAndName() {
        String phone = "91" + (System.currentTimeMillis() % 100000000L);
        assertThat(patientDAO.existsByPhoneAndName(phone, "Test", "User")).isFalse();

        Patient p = new Patient(null, patientDAO.getNextPatientId(), "Test", "User",
                LocalDate.of(1995, 1, 1), Gender.OTHER, phone, "Delhi");
        patientDAO.save(p);

        assertThat(patientDAO.existsByPhoneAndName(phone, "Test", "User")).isTrue();
    }
}
