package com.audino.dao;

import com.audino.model.PatientVitals;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for visit vitals.
 */
public interface VitalsDAO {

    PatientVitals save(PatientVitals vitals);

    Optional<PatientVitals> findLatestByPatientId(Long patientId);

    List<PatientVitals> findHistoryByPatientId(Long patientId, int limit);
}
