package com.audino;

import com.audino.dao.InteractionRuleDAO;
import com.audino.dao.jdbc.InteractionRuleDAOJdbc;
import com.audino.model.AlertLevel;
import com.audino.model.AlertType;
import com.audino.model.InteractionRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration Test: InteractionRuleDAO Native JDBC Operations")
class InteractionRuleDAOIT extends AbstractDAOIntegrationTest {

    private InteractionRuleDAO ruleDAO;

    @BeforeEach
    void setUp() {
        ruleDAO = new InteractionRuleDAOJdbc(dataSource);
    }

    @Test
    @DisplayName("findAll() returns populated seeded clinical CDS interaction rules")
    void testFindAllSeededRules() {
        List<InteractionRule> allRules = ruleDAO.findAll();
        assertThat(allRules).isNotEmpty();
        assertThat(allRules.size()).isGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("findByType() isolates rules by alert type category")
    void testFindByType() {
        List<InteractionRule> drugDrug = ruleDAO.findByType(AlertType.DRUG_DRUG);
        assertThat(drugDrug).isNotEmpty();
        assertThat(drugDrug).allMatch(r -> r.getType() == AlertType.DRUG_DRUG);

        List<InteractionRule> drugAllergy = ruleDAO.findByType(AlertType.DRUG_ALLERGY);
        assertThat(drugAllergy).isNotEmpty();
        assertThat(drugAllergy).allMatch(r -> r.getType() == AlertType.DRUG_ALLERGY);

        List<InteractionRule> drugCond = ruleDAO.findByType(AlertType.DRUG_CONDITION);
        assertThat(drugCond).isNotEmpty();
        assertThat(drugCond).allMatch(r -> r.getType() == AlertType.DRUG_CONDITION);
    }

    @Test
    @DisplayName("save(), update(), and delete() properly mutate interaction rules in real time")
    void testRuleCrudLifecycle() {
        String uniqueKey = "drug_" + UUID.randomUUID().toString().substring(0, 6);
        InteractionRule newRule = new InteractionRule(
                null,
                AlertType.DRUG_DRUG,
                uniqueKey,
                "warfarin",
                AlertLevel.CRITICAL,
                "Test severe interaction causing acute coagulopathy",
                "Do not co-prescribe; consider safer alternative"
        );

        InteractionRule saved = ruleDAO.save(newRule);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isPositive();

        // Verify retrieval
        Optional<InteractionRule> fetchedOpt = ruleDAO.findById(saved.getId());
        assertThat(fetchedOpt).isPresent();
        InteractionRule fetched = fetchedOpt.get();
        assertThat(fetched.getKeyword1()).isEqualTo(uniqueKey);
        assertThat(fetched.getSeverity()).isEqualTo(AlertLevel.CRITICAL);

        // Update
        InteractionRule updatedRule = new InteractionRule(
                saved.getId(),
                AlertType.DRUG_DRUG,
                uniqueKey,
                "warfarin",
                AlertLevel.WARNING,
                "Downgraded severity after clinical review",
                "Monitor INR closely if co-prescribed"
        );
        ruleDAO.update(updatedRule);

        InteractionRule postUpdate = ruleDAO.findById(saved.getId()).orElseThrow();
        assertThat(postUpdate.getSeverity()).isEqualTo(AlertLevel.WARNING);
        assertThat(postUpdate.getDescription()).contains("Downgraded severity");

        // Delete
        ruleDAO.delete(saved.getId());
        assertThat(ruleDAO.findById(saved.getId())).isEmpty();
    }
}
