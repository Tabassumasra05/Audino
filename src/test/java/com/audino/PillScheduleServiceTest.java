package com.audino;

import com.audino.model.Frequency;
import com.audino.model.Gender;
import com.audino.model.Medication;
import com.audino.model.Patient;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionItem;
import com.audino.model.TabletMedication;
import com.audino.service.PillScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Test: PillScheduleService & 4-Phase Chronological Timeline")
class PillScheduleServiceTest {

    @Test
    @DisplayName("Prescription items are mapped to correct 4-phase day slots (Morning, Afternoon, Evening, Night)")
    void testScheduleGridMapping() {
        PillScheduleService service = new PillScheduleService();

        Patient patient = new Patient(1L, "P-101", "Ramesh", "Sharma", LocalDate.of(1980, 1, 1), Gender.MALE, "9820112345", "Mumbai", List.of(), List.of(), null);
        Prescription rx = new Prescription(patient.getId(), "doctor", 1L);
        rx.setPatient(patient);

        Medication pantoprazole = new TabletMedication(1L, "Pantoprazole", "Pan 40", "40790", "40mg", List.of("Pantoprazole"), new BigDecimal("8.00"));
        Medication paracetamol = new TabletMedication(2L, "Paracetamol", "Dolo 650", "3222", "650mg", List.of("Paracetamol"), new BigDecimal("3.00"));
        Medication bedtimeMeds = new TabletMedication(3L, "Atorvastatin", "Atorva 10", "83367", "10mg", List.of("Atorvastatin"), new BigDecimal("7.00"));

        // Pan 40: ONCE_DAILY (Morning)
        rx.addItem(new PrescriptionItem(pantoprazole, "1 tablet", Frequency.ONCE_DAILY, 10, "Before food"));
        // Dolo 650: TWICE_DAILY (Morning & Night)
        rx.addItem(new PrescriptionItem(paracetamol, "1 tablet", Frequency.TWICE_DAILY, 5, "After food"));
        // Atorva 10: BEDTIME (Night)
        rx.addItem(new PrescriptionItem(bedtimeMeds, "1 tablet", Frequency.BEDTIME, 30, "At bedtime"));

        PillScheduleService.DailyPillSchedule schedule = service.generateSchedule(rx);

        assertThat(schedule).isNotNull();
        assertThat(schedule.patientName()).isEqualTo("Ramesh Sharma");

        // Morning slot: should contain Pan 40 and Dolo 650
        List<PillScheduleService.DoseEntry> morning = schedule.scheduleGrid().get(Frequency.DosePhase.MORNING);
        assertThat(morning).hasSize(2);
        assertThat(morning).anyMatch(e -> e.medicationName().equals("Pan 40") && e.instructions().contains("Before food"));
        assertThat(morning).anyMatch(e -> e.medicationName().equals("Dolo 650") && e.instructions().contains("After food"));

        // Night slot: should contain Dolo 650 and Atorva 10
        List<PillScheduleService.DoseEntry> night = schedule.scheduleGrid().get(Frequency.DosePhase.NIGHT);
        assertThat(night).hasSize(2);
        assertThat(night).anyMatch(e -> e.medicationName().equals("Dolo 650"));
        assertThat(night).anyMatch(e -> e.medicationName().equals("Atorva 10"));

        // Precautions extracted
        assertThat(schedule.precautions()).anyMatch(p -> p.contains("empty stomach"));
        assertThat(schedule.precautions()).anyMatch(p -> p.contains("strictly after meals"));
    }
}
