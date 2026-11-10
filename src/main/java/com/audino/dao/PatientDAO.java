package com.audino.dao;

import com.audino.model.Patient;
import com.audino.model.PatientAllergy;
import com.audino.model.PatientCondition;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for patient demographic and clinical history entities.
 */
public interface PatientDAO {

    Optional<Patient> findById(Long id);

    Optional<Patient> findByPatientId(String patientId);

    List<Patient> findAll();

    List<Patient> search(String query);

    Patient save(Patient patient);

    void update(Patient patient);

    List<PatientAllergy> findAllergiesByPatientId(Long patientId);

    List<PatientCondition> findConditionsByPatientId(Long patientId);

    boolean existsByPhoneAndName(String phone, String firstName, String lastName);

    String getNextPatientId();
}
