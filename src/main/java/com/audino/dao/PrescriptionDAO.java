package com.audino.dao;

import com.audino.model.Prescription;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for prescriptions, prescription line items, and interaction alerts.
 */
public interface PrescriptionDAO {

    /**
     * Atomically persists a prescription along with its owned items and alerts within a single ACID transaction.
     *
     * @param prescription The prescription entity to persist
     * @return The persisted prescription with generated primary keys
     */
    Prescription saveWithTransaction(Prescription prescription);

    Optional<Prescription> findById(Long id);

    Optional<Prescription> findByTokenId(Long tokenId);

    List<Prescription> findByPatientId(Long patientId);

    int countTodayPrescriptions();

    int countTodayCriticalOverrides();
}
