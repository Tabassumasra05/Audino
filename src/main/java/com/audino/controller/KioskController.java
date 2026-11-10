package com.audino.controller;

import com.audino.dao.PatientDAO;
import com.audino.dao.PrescriptionDAO;
import com.audino.dao.TokenDAO;
import com.audino.dao.VitalsDAO;
import com.audino.dao.jdbc.MedicationDAOJdbc;
import com.audino.dao.jdbc.PatientDAOJdbc;
import com.audino.dao.jdbc.PrescriptionDAOJdbc;
import com.audino.dao.jdbc.TokenDAOJdbc;
import com.audino.dao.jdbc.VitalsDAOJdbc;
import com.audino.model.Frequency;
import com.audino.model.Patient;
import com.audino.model.PatientVitals;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionItem;
import com.audino.model.Token;
import com.audino.service.PillScheduleService;
import com.audino.service.PrescriptionPdfService;
import com.audino.ui.components.NumericKeypad;
import com.audino.ui.components.Toast;
import com.audino.ui.navigation.NavigationService;
import com.audino.util.ConfigurationManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the Patient Self-Service Kiosk:
 * Implements dual-factor token authentication, 3-attempt 60s lockout, visual 4-phase chrono-grid
 * pill schedule, patient PDF download, and 120s inactivity auto-reset.
 */
public class KioskController {

    private static final Logger logger = LoggerFactory.getLogger(KioskController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    @FXML private StackPane kioskRoot;

    // STEP 1: VERIFICATION SCREEN
    @FXML private VBox verifyContainer;
    @FXML private TextField tokenNoField;
    @FXML private VBox keypadContainer;

    @FXML private RadioButton radioPhone;
    @FXML private RadioButton radioDob;
    @FXML private TextField phoneField;
    @FXML private DatePicker dobPicker;

    @FXML private Label lockoutWarningLabel;
    @FXML private Button verifyBtn;
    @FXML private Button returnToStaffBtn;

    // STEP 2: SCHEDULE & DISPENSING SUMMARY SCREEN
    @FXML private VBox scheduleContainer;
    @FXML private Label patientWelcomeLabel;
    @FXML private Label patientMetaLabel;
    @FXML private Label doctorConsultLabel;
    @FXML private Label inactivityTimerLabel;

    @FXML private VBox medicationCardsBox;
    @FXML private GridPane chronoGrid;

    @FXML private Button downloadPdfBtn;
    @FXML private Button doneBtn;

    private PatientDAO patientDAO;
    private TokenDAO tokenDAO;
    private PrescriptionDAO prescriptionDAO;
    private VitalsDAO vitalsDAO;
    private PillScheduleService pillScheduleService;
    private PrescriptionPdfService pdfService;

    // State Variables
    private int failedAttempts = 0;
    private int lockoutSecondsLeft = 0;
    private Timeline lockoutTimer;
    private int inactivitySecondsLeft = 120;
    private Timeline inactivityTimer;

    private Patient authenticatedPatient;
    private Prescription activePrescription;
    private PatientVitals patientVitals;

    @FXML
    public void initialize() {
        DataSource ds = ConfigurationManager.getInstance().getDataSource();
        this.patientDAO = new PatientDAOJdbc(ds);
        this.tokenDAO = new TokenDAOJdbc(ds);
        this.prescriptionDAO = new PrescriptionDAOJdbc(ds, new MedicationDAOJdbc(ds));
        this.vitalsDAO = new VitalsDAOJdbc(ds);
        this.pillScheduleService = new PillScheduleService();
        this.pdfService = new PrescriptionPdfService();

        // 1. Setup Touch Numeric Keypad targeting token input
        NumericKeypad keypad = new NumericKeypad(tokenNoField);
        keypadContainer.getChildren().add(keypad);

        // Target field focus switching for keypad
        tokenNoField.focusedProperty().addListener((obs, o, isFocused) -> {
            if (isFocused) keypad.setTargetField(tokenNoField);
        });
        phoneField.focusedProperty().addListener((obs, o, isFocused) -> {
            if (isFocused) keypad.setTargetField(phoneField);
        });

        // 2. Setup Dual-Factor Selector Radio Group
        ToggleGroup group = new ToggleGroup();
        radioPhone.setToggleGroup(group);
        radioDob.setToggleGroup(group);
        radioPhone.setSelected(true);

        group.selectedToggleProperty().addListener((obs, o, n) -> {
            boolean isPhone = radioPhone.isSelected();
            phoneField.setVisible(isPhone);
            phoneField.setManaged(isPhone);
            dobPicker.setVisible(!isPhone);
            dobPicker.setManaged(!isPhone);
        });

        dobPicker.setValue(LocalDate.of(1980, 1, 1));
        lockoutWarningLabel.setVisible(false);
        lockoutWarningLabel.setManaged(false);

        // 3. Button Handlers
        verifyBtn.setOnAction(e -> handleVerification());
        returnToStaffBtn.setOnAction(e -> {
            stopInactivityTimer();
            NavigationService.getInstance().switchRootScene("/fxml/login.fxml", "Staff Login");
        });

        doneBtn.setOnAction(e -> resetKioskToVerification());
        downloadPdfBtn.setOnAction(e -> handleDownloadPdf());

        // Inactivity Watchdog
        setupInactivityTimer();

        // Show step 1 initially
        showVerificationScreen();
    }

    private void handleVerification() {
        if (lockoutSecondsLeft > 0) {
            Toast.show(kioskRoot, "Terminal is temporarily locked. Please wait " + lockoutSecondsLeft + " seconds.", Toast.Type.ERROR);
            return;
        }

        String rawToken = tokenNoField.getText();
        if (rawToken == null || rawToken.isBlank()) {
            Toast.show(kioskRoot, "Please enter your Queue Token Number (e.g. 1 or T-001).", Toast.Type.WARNING);
            return;
        }

        // Normalize token number (e.g. "1" -> "T-001")
        String tokenNo = rawToken.trim().toUpperCase();
        if (!tokenNo.startsWith("T-")) {
            try {
                int num = Integer.parseInt(tokenNo);
                tokenNo = String.format("T-%03d", num);
            } catch (NumberFormatException ignored) {
            }
        }

        // Look up token for today
        Optional<Token> tokenOpt = tokenDAO.findByTokenNoAndDate(tokenNo, LocalDate.now());
        if (tokenOpt.isEmpty()) {
            registerFailedAttempt("Token '" + tokenNo + "' not found for today.");
            return;
        }

        Token token = tokenOpt.get();
        Optional<Patient> patientOpt = patientDAO.findById(token.getPatientId());
        if (patientOpt.isEmpty()) {
            registerFailedAttempt("Patient record associated with Token not found.");
            return;
        }

        Patient patient = patientOpt.get();

        // Factor 2: Verify Phone or Date of Birth
        boolean factor2Matches = false;
        if (radioPhone.isSelected()) {
            String enteredPhone = phoneField.getText() != null ? phoneField.getText().trim() : "";
            factor2Matches = !enteredPhone.isEmpty() && patient.getPhone().endsWith(enteredPhone);
        } else {
            LocalDate enteredDob = dobPicker.getValue();
            factor2Matches = enteredDob != null && enteredDob.equals(patient.getDateOfBirth());
        }

        if (!factor2Matches) {
            registerFailedAttempt("Second verification factor (Phone/DOB) did not match our records.");
            return;
        }

        // Successful Dual-Factor Authentication!
        failedAttempts = 0;
        this.authenticatedPatient = patient;

        // Retrieve prescription
        Optional<Prescription> rxOpt = prescriptionDAO.findByTokenId(token.getId());
        if (rxOpt.isEmpty()) {
            // Check latest approved prescription for patient
            List<Prescription> patientRxs = prescriptionDAO.findByPatientId(patient.getId());
            if (!patientRxs.isEmpty()) {
                rxOpt = Optional.of(patientRxs.get(0));
            }
        }

        if (rxOpt.isPresent()) {
            this.activePrescription = rxOpt.get();
        } else {
            this.activePrescription = null;
        }

        List<PatientVitals> vitalsList = vitalsDAO.findByPatientId(patient.getId());
        this.patientVitals = vitalsList.isEmpty() ? null : vitalsList.get(0);

        loadScheduleScreen();
    }

    private void registerFailedAttempt(String reason) {
        failedAttempts++;
        logger.warn("Kiosk failed verification attempt {} of 3: {}", failedAttempts, reason);

        if (failedAttempts >= 3) {
            startLockoutTimer();
        } else {
            Toast.show(kioskRoot, reason + " (" + (3 - failedAttempts) + " attempts remaining)", Toast.Type.ERROR);
        }
    }

    private void startLockoutTimer() {
        lockoutSecondsLeft = 60;
        verifyBtn.setDisable(true);
        lockoutWarningLabel.setVisible(true);
        lockoutWarningLabel.setManaged(true);

        lockoutTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            lockoutSecondsLeft--;
            lockoutWarningLabel.setText("🔒 Security Lockout Active: Too many failed attempts. Try again in " + lockoutSecondsLeft + "s.");
            if (lockoutSecondsLeft <= 0) {
                lockoutTimer.stop();
                failedAttempts = 0;
                verifyBtn.setDisable(false);
                lockoutWarningLabel.setVisible(false);
                lockoutWarningLabel.setManaged(false);
            }
        }));
        lockoutTimer.setCycleCount(Animation.INDEFINITE);
        lockoutTimer.play();
    }

    private void loadScheduleScreen() {
        verifyContainer.setVisible(false);
        verifyContainer.setManaged(false);

        scheduleContainer.setVisible(true);
        scheduleContainer.setManaged(true);

        patientWelcomeLabel.setText("Welcome, " + authenticatedPatient.getFullName());
        patientMetaLabel.setText("Patient ID: " + authenticatedPatient.getPatientId() + " • Age: " + authenticatedPatient.getAge() + " yrs • " + authenticatedPatient.getPhone());

        if (activePrescription != null) {
            doctorConsultLabel.setText("Prescribed by Dr. " + activePrescription.getDoctorUsername() + " on " + activePrescription.getCreatedAt().format(DATE_FMT));
            downloadPdfBtn.setDisable(false);
        } else {
            doctorConsultLabel.setText("No new prescription issued yet today. Please proceed to consultation room.");
            downloadPdfBtn.setDisable(true);
        }

        // Render Medication Cards List
        medicationCardsBox.getChildren().clear();
        if (activePrescription != null && !activePrescription.getItems().isEmpty()) {
            for (PrescriptionItem item : activePrescription.getItems()) {
                medicationCardsBox.getChildren().add(createMedicationCard(item));
            }
        } else {
            Label noMeds = new Label("No active prescription items found for today's visit.");
            noMeds.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-text-muted; -fx-font-style: italic;");
            medicationCardsBox.getChildren().add(noMeds);
        }

        // Render 4-Phase Chrono-Grid
        renderChronoGrid();

        // Reset Inactivity Timer
        resetInactivityCountdown();
    }

    private HBox createMedicationCard(PrescriptionItem item) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("surface-card");
        card.setPadding(new Insets(12, 16, 12, 16));

        StackPane pillIcon = new StackPane();
        pillIcon.setStyle("-fx-background-color: -color-primary-soft; -fx-background-radius: 8px; -fx-pref-width: 40px; -fx-pref-height: 40px;");
        try {
            FontIcon icon = new FontIcon("mdi2p-pill");
            icon.setIconSize(22);
            icon.setStyle("-fx-icon-color: -color-primary;");
            pillIcon.getChildren().add(icon);
        } catch (Exception ignored) {
        }

        VBox info = new VBox(3);
        String brand = item.getMedication() != null ? item.getMedication().getBrandName() : "Medication #" + item.getMedicationId();
        String gen = item.getMedication() != null ? item.getMedication().getGenericName() + " (" + item.getMedication().getStrength() + ")" : "";

        Label title = new Label(brand + "  •  " + gen);
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        Label doseFreq = new Label(item.getDosage() + " • " + item.getFrequency().getLabel() + " for " + item.getDurationDays() + " days");
        doseFreq.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -color-primary;");

        Label instr = new Label("Instructions: " + (item.getInstructions() != null ? item.getInstructions() : "As directed by physician"));
        instr.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-secondary;");

        info.getChildren().addAll(title, doseFreq, instr);
        HBox.setHgrow(info, Priority.ALWAYS);

        card.getChildren().addAll(pillIcon, info);
        return card;
    }

    private void renderChronoGrid() {
        chronoGrid.getChildren().clear();

        List<PillScheduleService.SchedulePhase> timeline = (activePrescription != null) ?
                pillScheduleService.generateTimeline(activePrescription) : List.of();

        int col = 0;
        for (PillScheduleService.SchedulePhase phase : timeline) {
            VBox colBox = new VBox(10);
            colBox.setStyle("-fx-background-color: -color-surface; -fx-border-color: -color-border; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 14px; -fx-min-width: 170px;");
            colBox.setAlignment(Pos.TOP_CENTER);
            GridPane.setHgrow(colBox, Priority.ALWAYS);

            // Phase Header
            HBox header = new HBox(6);
            header.setAlignment(Pos.CENTER);
            try {
                String iconName = switch (phase.phase()) {
                    case MORNING -> "mdi2w-weather-sunset-up";
                    case AFTERNOON -> "mdi2w-weather-sunny";
                    case EVENING -> "mdi2w-weather-sunset-down";
                    case NIGHT -> "mdi2w-weather-night";
                    case AS_NEEDED -> "mdi2f-flash";
                };
                FontIcon icon = new FontIcon(iconName);
                icon.setIconSize(18);
                icon.setStyle("-fx-icon-color: -color-primary;");
                header.getChildren().add(icon);
            } catch (Exception ignored) {
            }

            Label phaseTitle = new Label(phase.phaseTitle().toUpperCase());
            phaseTitle.setStyle("-fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: -color-text;");
            header.getChildren().add(phaseTitle);

            Label phaseTime = new Label(phase.timeTarget());
            phaseTime.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: -color-primary;");

            VBox doseList = new VBox(6);
            doseList.setAlignment(Pos.TOP_LEFT);

            if (phase.items().isEmpty()) {
                Label noDose = new Label("No pills");
                noDose.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-style: italic;");
                doseList.getChildren().add(noDose);
            } else {
                for (PillScheduleService.PhaseDoseItem item : phase.items()) {
                    VBox pillBadge = new VBox(2);
                    pillBadge.setStyle("-fx-background-color: -color-surface-alt; -fx-border-color: -color-border; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6px 8px;");
                    Label bName = new Label(item.brandName());
                    bName.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: -color-text;");
                    Label bDose = new Label(item.dosage());
                    bDose.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-primary;");
                    Label bMeal = new Label(item.mealPrecaution());
                    bMeal.setStyle("-fx-font-size: 9px; -fx-text-fill: -color-text-muted;");
                    pillBadge.getChildren().addAll(bName, bDose, bMeal);
                    doseList.getChildren().add(pillBadge);
                }
            }

            colBox.getChildren().addAll(header, phaseTime, new Separator(), doseList);
            chronoGrid.add(colBox, col++, 0);
        }
    }

    private void handleDownloadPdf() {
        if (activePrescription == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Prescription Schedule PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("My_Prescription_" + authenticatedPatient.getPatientId() + ".pdf");

        File file = chooser.showSaveDialog(kioskRoot.getScene().getWindow());
        if (file != null) {
            try {
                pdfService.generatePdf(activePrescription, patientVitals, file, false);
                Toast.show(kioskRoot, "Saved Prescription PDF to: " + file.getName(), Toast.Type.SUCCESS);
            } catch (Exception ex) {
                logger.error("Kiosk PDF export error: {}", ex.getMessage(), ex);
                Toast.show(kioskRoot, "PDF generation failed: " + ex.getMessage(), Toast.Type.ERROR);
            }
        }
    }

    private void setupInactivityTimer() {
        inactivityTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (scheduleContainer.isVisible()) {
                inactivitySecondsLeft--;
                inactivityTimerLabel.setText("Auto-Reset in " + inactivitySecondsLeft + "s");
                if (inactivitySecondsLeft <= 0) {
                    resetKioskToVerification();
                }
            }
        }));
        inactivityTimer.setCycleCount(Animation.INDEFINITE);
        inactivityTimer.play();

        // Any mouse click or key press resets the timer
        kioskRoot.setOnMouseMoved(e -> resetInactivityCountdown());
        kioskRoot.setOnKeyPressed(e -> resetInactivityCountdown());
    }

    private void resetInactivityCountdown() {
        inactivitySecondsLeft = 120;
    }

    private void stopInactivityTimer() {
        if (inactivityTimer != null) inactivityTimer.stop();
        if (lockoutTimer != null) lockoutTimer.stop();
    }

    private void resetKioskToVerification() {
        tokenNoField.clear();
        phoneField.clear();
        showVerificationScreen();
    }

    private void showVerificationScreen() {
        verifyContainer.setVisible(true);
        verifyContainer.setManaged(true);

        scheduleContainer.setVisible(false);
        scheduleContainer.setManaged(false);
    }
}
