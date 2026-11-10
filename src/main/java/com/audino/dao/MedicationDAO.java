package com.audino.dao;

import com.audino.model.Medication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for formulary medication entities.
 */
public interface MedicationDAO {

    Optional<Medication> findById(Long id);

    List<Medication> findAll();

    List<Medication> search(String query);

    Medication save(Medication medication);

    void update(Medication medication);

    void updatePrice(Long medicationId, BigDecimal newPrice);

    void delete(Long id);

    List<String> findIngredientsByMedicationId(Long medicationId);
}
