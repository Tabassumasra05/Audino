package com.audino.model;

import com.audino.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Encapsulates physiological vital signs captured during an outpatient visit.
 *
 * // OOM: Encapsulation – constructor and setters enforce clinical validation boundaries and compute derived metrics (BMI).
 */
public class PatientVitals {

    private Long id;
    private Long patientId;
    private LocalDateTime recordedAt;
    private double heightCm;
    private double weightKg;
    private int systolicBp;
    private int diastolicBp;
    private int pulseBpm;
    private double temperatureC;
    private int spo2Percent;

    public PatientVitals(Long id, Long patientId, LocalDateTime recordedAt,
                         double heightCm, double weightKg,
                         int systolicBp, int diastolicBp,
                         int pulseBpm, double temperatureC, int spo2Percent) {
        validateRanges(heightCm, weightKg, systolicBp, diastolicBp, pulseBpm, temperatureC, spo2Percent);
        this.id = id;
        this.patientId = patientId;
        this.recordedAt = recordedAt != null ? recordedAt : LocalDateTime.now();
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.pulseBpm = pulseBpm;
        this.temperatureC = temperatureC;
        this.spo2Percent = spo2Percent;
    }

    public PatientVitals(Long patientId, double heightCm, double weightKg,
                         int systolicBp, int diastolicBp,
                         int pulseBpm, double temperatureC, int spo2Percent) {
        this(null, patientId, LocalDateTime.now(), heightCm, weightKg, systolicBp, diastolicBp, pulseBpm, temperatureC, spo2Percent);
    }

    private static void validateRanges(double height, double weight, int systolic, int diastolic,
                                       int pulse, double temp, int spo2) {
        if (height < 30.0 || height > 250.0) {
            throw new IllegalArgumentException("Height must be between 30 and 250 cm (received: " + height + ")");
        }
        if (weight < 2.0 || weight > 300.0) {
            throw new IllegalArgumentException("Weight must be between 2 and 300 kg (received: " + weight + ")");
        }
        if (systolic < 50 || systolic > 260) {
            throw new IllegalArgumentException("Systolic BP must be between 50 and 260 mmHg (received: " + systolic + ")");
        }
        if (diastolic < 30 || diastolic > 180) {
            throw new IllegalArgumentException("Diastolic BP must be between 30 and 180 mmHg (received: " + diastolic + ")");
        }
        if (pulse < 30 || pulse > 220) {
            throw new IllegalArgumentException("Pulse must be between 30 and 220 bpm (received: " + pulse + ")");
        }
        if (temp < 33.0 || temp > 43.0) {
            throw new IllegalArgumentException("Temperature must be between 33.0 and 43.0 °C (received: " + temp + ")");
        }
        if (spo2 < 50 || spo2 > 100) {
            throw new IllegalArgumentException("SpO2 must be between 50 and 100% (received: " + spo2 + ")");
        }
    }

    /**
     * Calculates Body Mass Index: weight (kg) / (height (m))^2.
     *
     * @return BMI rounded to one decimal place
     */
    public double calculateBmi() {
        if (heightCm <= 0) return 0.0;
        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);
        return BigDecimal.valueOf(bmi).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Categorizes the calculated BMI according to WHO clinical thresholds.
     *
     * @return Classification label
     */
    public String getBmiCategory() {
        double bmi = calculateBmi();
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal";
        if (bmi < 30.0) return "Overweight";
        return "Obese";
    }

    public String getBpString() {
        return systolicBp + "/" + diastolicBp + " mmHg";
    }

    public String getBloodPressureString() {
        return getBpString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public int getSystolicBp() {
        return systolicBp;
    }

    public int getDiastolicBp() {
        return diastolicBp;
    }

    public int getPulseBpm() {
        return pulseBpm;
    }

    public double getTemperatureC() {
        return temperatureC;
    }

    public int getSpo2Percent() {
        return spo2Percent;
    }
}
