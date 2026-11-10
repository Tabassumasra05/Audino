package com.audino.service;

import com.audino.model.Frequency;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service synthesizing structured, patient-friendly 4-phase daily pill schedules
 * from clinical prescriptions and frequency domain metadata.
 *
 * // OOM: Single Responsibility Principle (SRP) – solely responsible for chronological medication schedule mapping.
 */
public class PillScheduleService {

    /**
     * An itemized dose card within a time phase.
     */
    public record DoseEntry(
        String medicationName,
        String genericName,
        String strength,
        String dosage,
        String instructions,
        int durationDays
    ) {}

    /**
     * Complete daily schedule organized into chronological phases.
     */
    public record DailyPillSchedule(
        String patientName,
        String doctorName,
        String tokenNo,
        Map<Frequency.DosePhase, List<DoseEntry>> scheduleGrid,
        List<String> precautions
    ) {}

    /**
     * Generates a patient-friendly 4-phase daily pill schedule from an approved prescription.
     *
     * @param prescription The prescription to map
     * @return Structured DailyPillSchedule object
     */
    public DailyPillSchedule generateSchedule(Prescription prescription) {
        Objects.requireNonNull(prescription, "Prescription must not be null");

        Map<Frequency.DosePhase, List<DoseEntry>> grid = new LinkedHashMap<>();
        grid.put(Frequency.DosePhase.MORNING, new ArrayList<>());
        grid.put(Frequency.DosePhase.AFTERNOON, new ArrayList<>());
        grid.put(Frequency.DosePhase.EVENING, new ArrayList<>());
        grid.put(Frequency.DosePhase.NIGHT, new ArrayList<>());
        grid.put(Frequency.DosePhase.AS_NEEDED, new ArrayList<>());

        List<String> precautions = new ArrayList<>();

        for (PrescriptionItem item : prescription.getItems()) {
            String medName = item.getMedication() != null ? item.getMedication().getBrandName() : "Medicine";
            String genName = item.getMedication() != null ? item.getMedication().getGenericName() : "";
            String str = item.getMedication() != null ? item.getMedication().getStrength() : "";
            String dose = item.getDosage();
            String instr = item.getInstructions() != null ? item.getInstructions() : "As directed";

            DoseEntry entry = new DoseEntry(medName, genName, str, dose, instr, item.getDurationDays());

            // Map into each scheduled phase according to Frequency enum
            Frequency freq = item.getFrequency();
            for (Frequency.DosePhase phase : freq.getScheduledPhases()) {
                grid.get(phase).add(entry);
            }

            // Extract cautionary advice
            if (instr.toLowerCase().contains("before")) {
                precautions.add("Take " + medName + " on an empty stomach (30 mins before food).");
            } else if (instr.toLowerCase().contains("after")) {
                precautions.add("Take " + medName + " strictly after meals to prevent gastric irritation.");
            }
        }

        String patName = prescription.getPatient() != null ? prescription.getPatient().getFullName() : "Patient";
        String docName = prescription.getPrescriber() != null ? prescription.getPrescriber().getFullName() : prescription.getDoctorUsername();
        String tokNo = prescription.getToken() != null ? prescription.getToken().getTokenNo() : "T-000";

        return new DailyPillSchedule(patName, docName, tokNo, grid, precautions);
    }
}
