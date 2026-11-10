package com.audino;

import com.audino.model.AlertLevel;
import com.audino.model.AlertType;
import com.audino.model.Doctor;
import com.audino.model.Frequency;
import com.audino.model.Gender;
import com.audino.model.Patient;
import com.audino.model.PatientVitals;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionItem;
import com.audino.model.PrescriptionStatus;
import com.audino.model.TabletMedication;
import com.audino.model.Token;
import com.audino.service.PrescriptionPdfService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Test: PrescriptionPdfService iText Generation")
class PrescriptionPdfServiceTest {

    @Test
    @DisplayName("generatePdf() produces valid clinical PDF document with items, vitals, and pill schedule")
    void testPdfGeneration(@TempDir Path tempDir) throws Exception {
        PrescriptionPdfService pdfService = new PrescriptionPdfService();

        // 1. Setup Mock Domain Data
        Patient patient = new Patient(101L, "P-1001", "Ramesh", "Sharma",
                LocalDate.of(1980, 5, 14), Gender.MALE, "9820112345", "Flat 402, Gokul Dham, Mumbai");
        Doctor doctor = new Doctor(1L, "doctor", "hash", "Dr. Arjun Mehta",
                "General Physician & Internal Medicine", "MCI-48291", true, LocalDateTime.now());
        Token token = new Token(1L, "T-001", LocalDate.now(), patient.getId(), doctor.getUsername(), null);

        Prescription rx = new Prescription(
                999L,
                patient.getId(),
                doctor.getUsername(),
                token.getId(),
                PrescriptionStatus.APPROVED,
                new BigDecimal("55.00"),
                LocalDateTime.now()
        );
        rx.setPatient(patient);
        rx.setPrescriber(doctor);
        rx.setToken(token);

        TabletMedication dolo = new TabletMedication(1L, "Paracetamol", "Dolo 650", "3222", "650mg",
                List.of("Paracetamol"), new BigDecimal("30.00"));
        PrescriptionItem item1 = new PrescriptionItem(1L, 999L, dolo.getId(), "1 tablet",
                Frequency.THRICE_DAILY, 5, "Take after meals with water", new BigDecimal("2.00"), 15, new BigDecimal("30.00"));
        item1.setMedication(dolo);
        rx.addItem(item1);

        TabletMedication brufen = new TabletMedication(2L, "Ibuprofen", "Brufen 400", "5640", "400mg",
                List.of("Ibuprofen"), new BigDecimal("25.00"));
        PrescriptionItem item2 = new PrescriptionItem(2L, 999L, brufen.getId(), "1 tablet",
                Frequency.TWICE_DAILY, 5, "Take with milk after breakfast and dinner", new BigDecimal("2.50"), 10, new BigDecimal("25.00"));
        item2.setMedication(brufen);
        rx.addItem(item2);

        // Add Overridden Alert
        PrescriptionAlert alert = new PrescriptionAlert(
                1L, 999L, AlertLevel.CRITICAL, AlertType.DRUG_DRUG,
                "Potential interaction between NSAID and other therapies",
                true, "Clinical override: Patient tolerates well with gastroprotective PPI",
                "doctor", LocalDateTime.now()
        );
        rx.addAlert(alert);

        PatientVitals vitals = new PatientVitals(
                1L, patient.getId(), LocalDateTime.now(),
                170.0, 70.0,
                120, 80, 72, 37.0, 98
        );

        // 2. Generate Patient Copy
        File patientPdf = tempDir.resolve("Prescription_Patient.pdf").toFile();
        String patientPdfPath = pdfService.generatePdf(rx, vitals, patientPdf, false);

        assertThat(patientPdf).exists();
        assertThat(patientPdfPath).isEqualTo(patientPdf.getAbsolutePath());
        assertThat(patientPdf.length()).isGreaterThan(1000L);

        // Check magic bytes %PDF-
        try (FileInputStream fis = new FileInputStream(patientPdf)) {
            byte[] header = new byte[5];
            int read = fis.read(header);
            assertThat(read).isEqualTo(5);
            assertThat(new String(header)).isEqualTo("%PDF-");
        }

        // 3. Generate Doctor Copy (includes safety override notes)
        File doctorPdf = tempDir.resolve("Prescription_Doctor.pdf").toFile();
        String doctorPdfPath = pdfService.generatePdf(rx, vitals, doctorPdf, true);

        assertThat(doctorPdf).exists();
        assertThat(doctorPdfPath).isEqualTo(doctorPdf.getAbsolutePath());
        assertThat(doctorPdf.length()).isGreaterThan(1000L);
    }
}
