package com.audino.dao;

import com.audino.model.AlertType;
import com.audino.model.InteractionRule;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for clinical decision support interaction rules.
 */
public interface InteractionRuleDAO {

    Optional<InteractionRule> findById(Long id);

    List<InteractionRule> findAll();

    List<InteractionRule> findByType(AlertType type);

    InteractionRule save(InteractionRule rule);

    void update(InteractionRule rule);

    void delete(Long id);
}
