package com.audino;

import com.audino.model.PatientVitals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit Test: Patient Vitals Validation & BMI Computation")
class VitalsValidationTest {

    @Test
    @DisplayName("Realistic vital signs pass validation and compute BMI accurately")
    void testValidVitalsAndBmi() {
        // Height: 175cm, Weight: 70kg -> BMI = 70 / (1.75 * 1.75) = 22.9 (Normal)
        PatientVitals vitals = new PatientVitals(1L, 175.0, 70.0, 120, 80, 72, 36.6, 98);

        assertThat(vitals.calculateBmi()).isEqualTo(22.9);
        assertThat(vitals.getBmiCategory()).isEqualTo("Normal");
        assertThat(vitals.getBpString()).isEqualTo("120/80 mmHg");
    }

    @Test
    @DisplayName("Overweight BMI category classification")
    void testOverweightBmi() {
        // Height: 165cm, Weight: 75kg -> BMI = 75 / (1.65^2) = 27.5 (Overweight)
        PatientVitals vitals = new PatientVitals(2L, 165.0, 75.0, 130, 85, 80, 36.8, 97);

        assertThat(vitals.calculateBmi()).isEqualTo(27.5);
        assertThat(vitals.getBmiCategory()).isEqualTo("Overweight");
    }

    @Test
    @DisplayName("Unrealistic Systolic Blood Pressure (< 50 or > 260) throws IllegalArgumentException")
    void testInvalidBloodPressureThrows() {
        assertThatThrownBy(() -> new PatientVitals(3L, 170.0, 65.0, 300, 80, 72, 36.5, 98))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Systolic BP");

        assertThatThrownBy(() -> new PatientVitals(3L, 170.0, 65.0, 40, 80, 72, 36.5, 98))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Systolic BP");
    }

    @Test
    @DisplayName("Unrealistic SpO2 (< 50 or > 100%) throws IllegalArgumentException")
    void testInvalidSpo2Throws() {
        assertThatThrownBy(() -> new PatientVitals(4L, 170.0, 65.0, 120, 80, 72, 36.5, 105))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SpO2");
    }

    @Test
    @DisplayName("Extreme body temperature (< 33.0 or > 43.0 °C) throws IllegalArgumentException")
    void testInvalidTemperatureThrows() {
        assertThatThrownBy(() -> new PatientVitals(5L, 170.0, 65.0, 120, 80, 72, 45.0, 98))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Temperature");
    }
}
