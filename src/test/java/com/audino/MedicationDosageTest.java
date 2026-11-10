package com.audino;

import com.audino.model.CapsuleMedication;
import com.audino.model.InjectionMedication;
import com.audino.model.LiquidMedication;
import com.audino.model.Medication;
import com.audino.model.TabletMedication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Test: Polymorphic Medication Dosage Validation & Unit Calculations")
class MedicationDosageTest {

    @Test
    @DisplayName("TabletMedication validates counts and metric strengths; rejects negative and extreme counts")
    void testTabletMedicationDosage() {
        Medication tablet = new TabletMedication(1L, "Paracetamol", "Dolo 650", "3222", "650mg", List.of("Paracetamol"), new BigDecimal("3.00"));

        // Valid dosages
        assertThat(tablet.isValidDosage("1")).isTrue();
        assertThat(tablet.isValidDosage("1 tablet")).isTrue();
        assertThat(tablet.isValidDosage("2 tablets")).isTrue();
        assertThat(tablet.isValidDosage("0.5 tablet")).isTrue();
        assertThat(tablet.isValidDosage("650mg")).isTrue();

        // Invalid dosages
        assertThat(tablet.isValidDosage("")).isFalse();
        assertThat(tablet.isValidDosage(null)).isFalse();
        assertThat(tablet.isValidDosage("0 tablets")).isFalse();
        assertThat(tablet.isValidDosage("50 tablets")).isFalse(); // Exceeds safe limit
        assertThat(tablet.isValidDosage("-2")).isFalse();

        // Units and cost calculation: 2 tablets * 3 times/day * 5 days = 30 tablets @ 3.00 = 90.00
        int units = tablet.calculateUnits("2 tablets", 3, 5);
        assertThat(units).isEqualTo(30);
        assertThat(tablet.calculateCost("2 tablets", 3, 5)).isEqualByComparingTo(new BigDecimal("90.00"));
    }

    @Test
    @DisplayName("CapsuleMedication validates whole capsule counts; rejects fractions")
    void testCapsuleMedicationDosage() {
        Medication capsule = new CapsuleMedication(2L, "Amoxicillin", "Mox 500", "723", "500mg", List.of("Amoxicillin"), new BigDecimal("7.50"));

        // Valid dosages
        assertThat(capsule.isValidDosage("1")).isTrue();
        assertThat(capsule.isValidDosage("1 capsule")).isTrue();
        assertThat(capsule.isValidDosage("2 capsules")).isTrue();

        // Invalid dosages (capsules cannot be split)
        assertThat(capsule.isValidDosage("0.5 capsule")).isFalse();
        assertThat(capsule.isValidDosage("0")).isFalse();
        assertThat(capsule.isValidDosage("10 capsules")).isFalse(); // Exceeds safe limit

        // Units: 1 capsule * 2 times/day * 7 days = 14 capsules
        int units = capsule.calculateUnits("1 capsule", 2, 7);
        assertThat(units).isEqualTo(14);
        assertThat(capsule.calculateCost("1 capsule", 2, 7)).isEqualByComparingTo(new BigDecimal("105.00"));
    }

    @Test
    @DisplayName("LiquidMedication validates ml, tsp, and tbsp notations; rejects extreme volumes")
    void testLiquidMedicationDosage() {
        Medication liquid = new LiquidMedication(3L, "Paracetamol Pediatric", "Calpol 120", "3222", "120mg/5ml", List.of("Paracetamol"), new BigDecimal("0.50"));

        // Valid dosages
        assertThat(liquid.isValidDosage("5ml")).isTrue();
        assertThat(liquid.isValidDosage("10ml")).isTrue();
        assertThat(liquid.isValidDosage("2.5ml")).isTrue();
        assertThat(liquid.isValidDosage("1 tsp")).isTrue();
        assertThat(liquid.isValidDosage("2 tbsp")).isTrue();

        // Invalid dosages
        assertThat(liquid.isValidDosage("0.1ml")).isFalse();
        assertThat(liquid.isValidDosage("500ml")).isFalse(); // Unrealistic single dose
        assertThat(liquid.isValidDosage("invalid")).isFalse();

        // Units: 10ml * 3 times/day * 5 days = 150ml
        int units = liquid.calculateUnits("10ml", 3, 5);
        assertThat(units).isEqualTo(150);
        assertThat(liquid.calculateCost("10ml", 3, 5)).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    @DisplayName("InjectionMedication validates vial and ml parenteral dosages")
    void testInjectionMedicationDosage() {
        Medication injection = new InjectionMedication(4L, "Ceftriaxone", "Monocef 1g", "2193", "1g", List.of("Ceftriaxone"), new BigDecimal("75.00"));

        // Valid dosages
        assertThat(injection.isValidDosage("1 vial")).isTrue();
        assertThat(injection.isValidDosage("2 vials")).isTrue();
        assertThat(injection.isValidDosage("1 ampoule")).isTrue();
        assertThat(injection.isValidDosage("2ml")).isTrue();

        // Invalid dosages
        assertThat(injection.isValidDosage("0 vials")).isFalse();
        assertThat(injection.isValidDosage("10 vials")).isFalse();
        assertThat(injection.isValidDosage("100ml")).isFalse();

        // Units: 1 vial * 1 time/day * 3 days = 3 vials
        int units = injection.calculateUnits("1 vial", 1, 3);
        assertThat(units).isEqualTo(3);
        assertThat(injection.calculateCost("1 vial", 1, 3)).isEqualByComparingTo(new BigDecimal("225.00"));
    }
}
