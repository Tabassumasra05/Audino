package com.audino.service;

import com.audino.model.Frequency;
import com.audino.model.PatientVitals;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionItem;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service generating high-fidelity clinical prescription PDFs using iText 5.
 *
 * // OOM: Single Responsibility Principle (SRP) – solely responsible for formatting and generating PDF output.
 */
public class PrescriptionPdfService {

    private static final Logger logger = LoggerFactory.getLogger(PrescriptionPdfService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    // Corporate Clinical Teal & Slate Palette
    private static final BaseColor COLOR_TEAL = new BaseColor(15, 118, 110);
    private static final BaseColor COLOR_DARK = new BaseColor(15, 23, 42);
    private static final BaseColor COLOR_MUTED = new BaseColor(100, 116, 139);
    private static final BaseColor COLOR_SURFACE_ALT = new BaseColor(248, 250, 252);
    private static final BaseColor COLOR_BORDER = new BaseColor(226, 232, 240);
    private static final BaseColor COLOR_CRITICAL_BG = new BaseColor(254, 226, 226);
    private static final BaseColor COLOR_CRITICAL_TEXT = new BaseColor(220, 38, 38);

    private static final Font FONT_HEADER = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, COLOR_TEAL);
    private static final Font FONT_SUBHEADER = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, COLOR_MUTED);
    private static final Font FONT_TITLE = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_DARK);
    private static final Font FONT_BODY = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, COLOR_DARK);
    private static final Font FONT_BODY_BOLD = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, COLOR_DARK);
    private static final Font FONT_MUTED = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, COLOR_MUTED);
    private static final Font FONT_TABLE_HEADER = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
    private static final Font FONT_CRITICAL = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, COLOR_CRITICAL_TEXT);

    private final PillScheduleService pillScheduleService = new PillScheduleService();

    /**
     * Compiles an official clinical prescription PDF.
     *
     * @param prescription The approved prescription
     * @param vitals Latest visit vitals (optional)
     * @param destinationFile File output destination
     * @param isDoctorCopy If true, includes overridden safety radar justification notes
     * @return Absolute path to generated PDF
     */
    public String generatePdf(Prescription prescription, PatientVitals vitals, File destinationFile, boolean isDoctorCopy) {
        Objects.requireNonNull(prescription, "Prescription must not be null");
        Objects.requireNonNull(destinationFile, "Destination file must not be null");

        // Ensure parent directories exist
        File parent = destinationFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
            PdfWriter.getInstance(document, fos);
            document.open();

            // 1. Clinic Header
            addClinicHeader(document);
            document.add(new Paragraph(" "));

            // 2. Doctor and Patient Identification Columns
            addPrescriberAndPatientBox(document, prescription, vitals);
            document.add(new Paragraph(" "));

            // 3. Medication Rx Table
            addMedicationsTable(document, prescription);
            document.add(new Paragraph(" "));

            // 4. Chronological 4-Phase Daily Pill Schedule Grid
            addDailyScheduleGrid(document, prescription);
            document.add(new Paragraph(" "));

            // 5. Overridden Warnings (Doctor / EMR Audit Copy Only)
            if (isDoctorCopy && !prescription.getAlerts().isEmpty()) {
                addOverrideAuditBox(document, prescription);
                document.add(new Paragraph(" "));
            }

            // 6. Signature Block and Disclaimer
            addSignatureAndFooter(document, prescription);

            document.close();
            logger.info("Prescription PDF generated successfully at: {}", destinationFile.getAbsolutePath());
            return destinationFile.getAbsolutePath();
        } catch (Exception e) {
            logger.error("Failed to generate prescription PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Prescription PDF compilation failed", e);
        }
    }

    private void addClinicHeader(Document doc) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{65, 35});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("AUDINO HEALTHCARE CLINIC", FONT_HEADER));
        leftCell.addElement(new Paragraph("Integrated Outpatient Clinical Care & Pharmacovigilance", FONT_SUBHEADER));
        leftCell.addElement(new Paragraph("Plot 42, Health City Parkway, Sector 12 | Phone: +91 (022) 5550-1920", FONT_MUTED));
        table.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph p = new Paragraph("OFFICIAL PRESCRIPTION", FONT_TITLE);
        p.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(p);
        Paragraph p2 = new Paragraph("Computerized EMR Generated", FONT_MUTED);
        p2.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(p2);
        table.addCell(rightCell);

        doc.add(table);

        LineSeparator ls = new LineSeparator(1.5f, 100, COLOR_TEAL, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(ls));
    }

    private void addPrescriberAndPatientBox(Document doc, Prescription rx, PatientVitals vitals) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});

        // Doctor Box
        PdfPCell docCell = new PdfPCell();
        docCell.setBackgroundColor(COLOR_SURFACE_ALT);
        docCell.setBorderColor(COLOR_BORDER);
        docCell.setPadding(8);

        String docName = rx.getPrescriber() != null ? rx.getPrescriber().getFullName() : "Dr. " + rx.getDoctorUsername();
        String docSpec = rx.getPrescriber() != null ? rx.getPrescriber().getSpecialization() : "Consulting Physician";
        String regNo = rx.getPrescriber() != null ? rx.getPrescriber().getRegistrationNumber() : "NMC-VERIFIED";

        docCell.addElement(new Paragraph("PRESCRIBING PHYSICIAN", FONT_SUBHEADER));
        docCell.addElement(new Paragraph(docName, FONT_TITLE));
        docCell.addElement(new Paragraph(docSpec, FONT_BODY));
        docCell.addElement(new Paragraph("Medical Reg No: " + regNo, FONT_BODY_BOLD));
        table.addCell(docCell);

        // Patient Box
        PdfPCell patCell = new PdfPCell();
        patCell.setBackgroundColor(COLOR_SURFACE_ALT);
        patCell.setBorderColor(COLOR_BORDER);
        patCell.setPadding(8);

        String patName = rx.getPatient() != null ? rx.getPatient().getFullName() : "Patient #" + rx.getPatientId();
        String patCode = rx.getPatient() != null ? rx.getPatient().getPatientId() : "P-000";
        String patMeta = rx.getPatient() != null ? (rx.getPatient().getAge() + " yrs / " + rx.getPatient().getGender()) : "";
        String tokNo = rx.getToken() != null ? rx.getToken().getTokenNo() : "T-000";

        patCell.addElement(new Paragraph("PATIENT DETAILS", FONT_SUBHEADER));
        patCell.addElement(new Paragraph(patName + " (" + patCode + ")", FONT_TITLE));
        patCell.addElement(new Paragraph("Age/Gender: " + patMeta + " | Token: " + tokNo, FONT_BODY));
        patCell.addElement(new Paragraph("Date: " + rx.getCreatedAt().format(DATE_FMT), FONT_BODY));

        if (vitals != null) {
            String vitTxt = "Vitals: BP " + vitals.getBpString() + " | Pulse: " + vitals.getPulseBpm() + " bpm | SpO2: " +
                            vitals.getSpo2Percent() + "% | BMI: " + vitals.calculateBmi();
            patCell.addElement(new Paragraph(vitTxt, FONT_MUTED));
        }

        table.addCell(patCell);
        doc.add(table);
    }

    private void addMedicationsTable(Document doc, Prescription rx) throws Exception {
        Paragraph p = new Paragraph("Rx - PRESCRIBED MEDICATIONS", FONT_TITLE);
        p.setSpacingAfter(6);
        doc.add(p);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{6, 34, 18, 18, 12, 12});

        String[] headers = {"#", "Medicine (Brand & Generic)", "Dosage", "Frequency", "Duration", "Qty"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_TABLE_HEADER));
            cell.setBackgroundColor(COLOR_TEAL);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        int index = 1;
        for (PrescriptionItem item : rx.getItems()) {
            BaseColor rowBg = (index % 2 == 0) ? COLOR_SURFACE_ALT : BaseColor.WHITE;

            addCell(table, String.valueOf(index++), FONT_BODY, Element.ALIGN_CENTER, rowBg);

            String medDetails = item.getMedicationName() + "\n" + (item.getInstructions() != null ? "(" + item.getInstructions() + ")" : "");
            addCell(table, medDetails, FONT_BODY, Element.ALIGN_LEFT, rowBg);

            addCell(table, item.getDosage(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
            addCell(table, item.getFrequency().getLabel(), FONT_BODY, Element.ALIGN_CENTER, rowBg);
            addCell(table, item.getDurationDays() + " days", FONT_BODY, Element.ALIGN_CENTER, rowBg);
            addCell(table, String.valueOf(item.getTotalUnits()), FONT_BODY_BOLD, Element.ALIGN_CENTER, rowBg);
        }

        doc.add(table);
    }

    private void addDailyScheduleGrid(Document doc, Prescription rx) throws Exception {
        Paragraph p = new Paragraph("DAILY DOSING SCHEDULE (4-PHASE TIMELINE)", FONT_TITLE);
        p.setSpacingAfter(6);
        doc.add(p);

        PillScheduleService.DailyPillSchedule schedule = pillScheduleService.generateSchedule(rx);
        Map<Frequency.DosePhase, List<PillScheduleService.DoseEntry>> grid = schedule.scheduleGrid();

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25, 25, 25, 25});

        Frequency.DosePhase[] phases = {
            Frequency.DosePhase.MORNING,
            Frequency.DosePhase.AFTERNOON,
            Frequency.DosePhase.EVENING,
            Frequency.DosePhase.NIGHT
        };

        // Phase Header Cards
        for (Frequency.DosePhase phase : phases) {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(COLOR_SURFACE_ALT);
            cell.setBorderColor(COLOR_BORDER);
            cell.setPadding(6);
            cell.addElement(new Paragraph(phase.getTitle().toUpperCase(), FONT_BODY_BOLD));
            cell.addElement(new Paragraph(phase.getDefaultTime(), FONT_MUTED));

            List<PillScheduleService.DoseEntry> entries = grid.get(phase);
            if (entries != null && !entries.isEmpty()) {
                for (PillScheduleService.DoseEntry d : entries) {
                    Paragraph dp = new Paragraph("• " + d.medicationName() + " (" + d.dosage() + ")", FONT_BODY);
                    cell.addElement(dp);
                    Paragraph ip = new Paragraph("  " + d.instructions(), FONT_MUTED);
                    cell.addElement(ip);
                }
            } else {
                cell.addElement(new Paragraph("— No medication —", FONT_MUTED));
            }
            table.addCell(cell);
        }

        doc.add(table);
    }

    private void addOverrideAuditBox(Document doc, Prescription rx) throws Exception {
        boolean hasOverrides = false;
        for (PrescriptionAlert a : rx.getAlerts()) {
            if (a.isCritical() && a.isOverridden()) {
                hasOverrides = true;
                break;
            }
        }
        if (!hasOverrides) return;

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_CRITICAL_BG);
        cell.setBorderColor(COLOR_CRITICAL_TEXT);
        cell.setPadding(8);

        cell.addElement(new Paragraph("DOCUMENTED CLINICAL OVERRIDES (PHYSICIAN AUDIT COPY)", FONT_BODY_BOLD));
        for (PrescriptionAlert a : rx.getAlerts()) {
            if (a.isCritical() && a.isOverridden()) {
                String txt = "• [" + a.getType() + "] " + a.getMessage() + "\n" +
                             "  Clinical Rationale: \"" + a.getOverrideNote() + "\" (Overridden by Dr. " +
                             a.getOverriddenBy() + ")";
                cell.addElement(new Paragraph(txt, FONT_CRITICAL));
            }
        }
        table.addCell(cell);
        doc.add(table);
    }

    private void addSignatureAndFooter(Document doc, Prescription rx) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{60, 40});

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph("Important Notice:", FONT_BODY_BOLD));
        left.addElement(new Paragraph("1. Take medications strictly as scheduled above.", FONT_MUTED));
        left.addElement(new Paragraph("2. In case of unexpected allergic reaction, report immediately.", FONT_MUTED));
        left.addElement(new Paragraph("3. This is a computer-verified clinical prescription.", FONT_MUTED));
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Paragraph("\n\n____________________________", FONT_BODY));
        String docName = rx.getPrescriber() != null ? rx.getPrescriber().getFullName() : "Dr. " + rx.getDoctorUsername();
        String reg = rx.getPrescriber() != null ? rx.getPrescriber().getRegistrationNumber() : "Reg Verified";
        Paragraph p = new Paragraph(docName + "\nReg No: " + reg, FONT_BODY_BOLD);
        p.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(p);
        table.addCell(right);

        doc.add(table);
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBackgroundColor(bg);
        cell.setBorderColor(COLOR_BORDER);
        table.addCell(cell);
    }
}
