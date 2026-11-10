package com.audino.controller;

import com.audino.dao.MedicationDAO;
import com.audino.dao.PatientDAO;
import com.audino.dao.PrescriptionDAO;
import com.audino.dao.TokenDAO;
import com.audino.dao.VitalsDAO;
import com.audino.dao.jdbc.AccountDAOJdbc;
import com.audino.dao.jdbc.AuditLogDAOJdbc;
import com.audino.dao.jdbc.InteractionRuleDAOJdbc;
import com.audino.dao.jdbc.MedicationDAOJdbc;
import com.audino.dao.jdbc.PatientDAOJdbc;
import com.audino.dao.jdbc.PrescriptionDAOJdbc;
import com.audino.dao.jdbc.TokenDAOJdbc;
import com.audino.dao.jdbc.VitalsDAOJdbc;
import com.audino.exception.CriticalInteractionException;
import com.audino.exception.InvalidDosageException;
import com.audino.exception.InvalidTokenStateException;
import com.audino.exception.ValidationException;
import com.audino.model.AlertLevel;
import com.audino.model.Doctor;
import com.audino.model.Frequency;
import com.audino.model.Medication;
import com.audino.model.Patient;
import com.audino.model.PatientVitals;
import com.audino.model.Prescription;
import com.audino.model.PrescriptionAlert;
import com.audino.model.PrescriptionItem;
import com.audino.model.PrescriptionStatus;
import com.audino.model.Token;
import com.audino.model.TokenStatus;
import com.audino.observer.QueueObserver;
import com.audino.service.AuditService;
import com.audino.service.MedicationSearchService;
import com.audino.service.PrescriptionPdfService;
import com.audino.service.PrescriptionService;
import com.audino.service.QueueService;
import com.audino.service.strategy.SafetyEngine;
import com.audino.ui.components.AlertCard;
import com.audino.ui.components.Chip;
import com.audino.ui.components.ConfirmDialog;
import com.audino.ui.components.EmptyState;
import com.audino.ui.components.PatientBanner;
import com.audino.ui.components.SearchField;
import com.audino.ui.components.Sparkline;
import com.audino.ui.components.Toast;
import com.audino.util.ConfigurationManager;
import com.audino.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the Doctor Ergonomic Prescribing & Clinical Decision Support Workspace:
 * Features live queue drawer, sticky safety banner, typo-autocorrect search, real-time safety radar,
 * mandatory override justification, and PDF export.
 */
public class DoctorConsultationController implements QueueObserver {

    private static final Logger logger = LoggerFactory.getLogger(DoctorConsultationController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    @FXML private StackPane doctorRootPane;
    @FXML private VBox bannerContainer;
    private PatientBanner patientBanner;

    // Queue Drawer
    @FXML private TableView<Token> queueTable;
    @FXML private TableColumn<Token, String> queueColToken;
    @FXML private TableColumn<Token, String> queueColPatient;
    @FXML private TableColumn<Token, String> queueColStatus;
    @FXML private Button callNextBtn;

    // Tab 1: Vitals & Trends
    @FXML private TableView<PatientVitals> vitalsTable;
    @FXML private TableColumn<PatientVitals, String> vitalsColDate;
    @FXML private TableColumn<PatientVitals, String> vitalsColBp;
    @FXML private TableColumn<PatientVitals, String> vitalsColPulse;
    @FXML private TableColumn<PatientVitals, String> vitalsColSpo2;
    @FXML private TableColumn<PatientVitals, String> vitalsColTemp;
    @FXML private TableColumn<PatientVitals, String> vitalsColBmi;
    @FXML private HBox sparklinesContainer;

    // Tab 2: Prescribing & Safety Radar
    @FXML private SearchField medSearchField;
    @FXML private HBox didYouMeanContainer;
    @FXML private Label didYouMeanLabel;
    @FXML private TableView<Medication> formularyTable;
    @FXML private TableColumn<Medication, String> formColBrand;
    @FXML private TableColumn<Medication, String> formColGeneric;
    @FXML private TableColumn<Medication, String> formColType;
    @FXML private TableColumn<Medication, String> formColStrength;
    @FXML private TableColumn<Medication, String> formColPrice;

    @FXML private TextField dosageField;
    @FXML private ComboBox<Frequency> frequencyCombo;
    @FXML private TextField durationField;
    @FXML private TextField instructionsField;
    @FXML private Button addItemBtn;

    @FXML private TableView<PrescriptionItem> rxItemsTable;
    @FXML private TableColumn<PrescriptionItem, String> rxColMedName;
    @FXML private TableColumn<PrescriptionItem, String> rxColDosage;
    @FXML private TableColumn<PrescriptionItem, String> rxColFrequency;
    @FXML private TableColumn<PrescriptionItem, String> rxColDuration;
    @FXML private TableColumn<PrescriptionItem, String> rxColUnits;
    @FXML private TableColumn<PrescriptionItem, String> rxColCost;
    @FXML private TableColumn<PrescriptionItem, Void> rxColAction;

    @FXML private Label totalCostLabel;

    // Safety Radar
    @FXML private VBox safetyRadarBox;
    @FXML private HBox blockingBanner;
    @FXML private Label blockingBannerLabel;
    @FXML private VBox alertsListContainer;

    @FXML private Button approveRxBtn;
    @FXML private Button downloadPdfBtn;

    // Tab 3: History
    @FXML private TableView<Prescription> historyTable;
    @FXML private TableColumn<Prescription, String> histColDate;
    @FXML private TableColumn<Prescription, String> histColDoctor;
    @FXML private TableColumn<Prescription, String> histColItems;
    @FXML private TableColumn<Prescription, String> histColCost;

    private PatientDAO patientDAO;
    private TokenDAO tokenDAO;
    private MedicationDAO medicationDAO;
    private VitalsDAO vitalsDAO;
    private PrescriptionDAO prescriptionDAO;
    private SafetyEngine safetyEngine;
    private PrescriptionService prescriptionService;
    private QueueService queueService;
    private MedicationSearchService searchService;
    private PrescriptionPdfService pdfService;

    private Token activeToken;
    private Patient activePatient;
    private PatientVitals latestVitals;
    private Prescription currentPrescription;

    private final ObservableList<Token> doctorQueue = FXCollections.observableArrayList();
    private final ObservableList<Medication> formularyResults = FXCollections.observableArrayList();
    private final ObservableList<PrescriptionItem> prescriptionItems = FXCollections.observableArrayList();
    private final ObservableList<PatientVitals> vitalsHistory = FXCollections.observableArrayList();
    private final ObservableList<Prescription> pastPrescriptions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        DataSource ds = ConfigurationManager.getInstance().getDataSource();
        this.patientDAO = new PatientDAOJdbc(ds);
        this.tokenDAO = new TokenDAOJdbc(ds);
        this.medicationDAO = new MedicationDAOJdbc(ds);
        this.vitalsDAO = new VitalsDAOJdbc(ds);
        this.prescriptionDAO = new PrescriptionDAOJdbc(ds, medicationDAO);
        InteractionRuleDAOJdbc ruleDAO = new InteractionRuleDAOJdbc(ds);
        this.safetyEngine = new SafetyEngine(ruleDAO);
        AuditService auditService = new AuditService(new AuditLogDAOJdbc(ds));
        this.prescriptionService = new PrescriptionService(prescriptionDAO, safetyEngine, auditService);
        this.queueService = new QueueService(tokenDAO, patientDAO);
        this.searchService = new MedicationSearchService(medicationDAO);
        this.pdfService = new PrescriptionPdfService();

        // Register as Observer for Queue Updates
        queueService.addObserver(this);

        initPatientBanner();
        initQueueDrawer();
        initVitalsTab();
        initPrescribingTab();
        initHistoryTab();

        reloadQueue();
    }

    private void initPatientBanner() {
        patientBanner = new PatientBanner();
        bannerContainer.getChildren().add(patientBanner);
        patientBanner.setPatient(null, null);
    }

    private void initQueueDrawer() {
        queueColToken.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTokenNo()));
        queueColPatient.setCellValueFactory(c -> {
            Optional<Patient> p = patientDAO.findById(c.getValue().getPatientId());
            return new SimpleStringProperty(p.map(Patient::getFullName).orElse("Patient #" + c.getValue().getPatientId()));
        });
        queueColStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Token t = getTableRow().getItem();
                    Chip.Variant v = t.getStatus() == TokenStatus.IN_CONSULTATION ? Chip.Variant.PRIMARY : Chip.Variant.WARNING;
                    Chip chip = new Chip(t.getStatus().name().replace('_', ' '), v);
                    setGraphic(chip);
                }
            }
        });

        queueTable.setItems(doctorQueue);
        queueTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) loadTokenConsultation(n);
        });

        callNextBtn.setOnAction(e -> handleCallNext());
    }

    private void reloadQueue() {
        Doctor doctor = SessionManager.getInstance().getCurrentDoctor();
        String docUser = doctor != null ? doctor.getUsername() : "doctor";
        List<Token> tokens = tokenDAO.findTodayTokensByDoctor(docUser).stream()
                .filter(t -> t.getStatus() == TokenStatus.WAITING || t.getStatus() == TokenStatus.IN_CONSULTATION)
                .toList();
        doctorQueue.setAll(tokens);
    }

    private void handleCallNext() {
        Doctor doctor = SessionManager.getInstance().getCurrentDoctor();
        String docUser = doctor != null ? doctor.getUsername() : "doctor";
        try {
            Optional<Token> nextOpt = queueService.callNextPatient(docUser);

            if (nextOpt.isPresent()) {
                Token t = nextOpt.get();
                reloadQueue();
                loadTokenConsultation(t);
                Toast.show(doctorRootPane, "Called Patient Token #" + t.getTokenNo() + " into consultation room.", Toast.Type.SUCCESS);
            } else {
                Toast.show(doctorRootPane, "No patients currently waiting in your queue.", Toast.Type.INFO);
            }
        } catch (InvalidTokenStateException ex) {
            logger.warn("Could not transition token: {}", ex.getMessage());
            Toast.show(doctorRootPane, "Cannot call token: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    public void loadTokenConsultation(Token token) {
        this.activeToken = token;
        Optional<Patient> patientOpt = patientDAO.findById(token.getPatientId());
        if (patientOpt.isEmpty()) {
            Toast.show(doctorRootPane, "Could not find patient record for Token " + token.getTokenNo(), Toast.Type.ERROR);
            return;
        }

        this.activePatient = patientOpt.get();
        List<PatientVitals> vitalsList = vitalsDAO.findByPatientId(activePatient.getId());
        this.latestVitals = vitalsList.isEmpty() ? null : vitalsList.get(0);

        // Update sticky banner
        patientBanner.setPatient(activePatient, latestVitals);

        // Reset or load prescription
        Doctor doc = SessionManager.getInstance().getCurrentDoctor();
        String docUser = doc != null ? doc.getUsername() : "doctor";
        this.currentPrescription = new Prescription(activePatient.getId(), docUser, activeToken.getId());
        this.currentPrescription.setPatient(activePatient);
        this.currentPrescription.setPrescriber(doc);
        this.currentPrescription.setToken(activeToken);

        prescriptionItems.clear();
        updatePrescriptionTotalsAndSafety();

        // Load vitals history & sparklines
        vitalsHistory.setAll(vitalsList);
        updateSparklines(vitalsList);

        // Load past prescriptions
        pastPrescriptions.setAll(prescriptionDAO.findByPatientId(activePatient.getId()));
    }

    private void initVitalsTab() {
        vitalsColDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRecordedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))));
        vitalsColBp.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBloodPressureString()));
        vitalsColPulse.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPulseBpm() + " bpm"));
        vitalsColSpo2.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSpo2Percent() + "%"));
        vitalsColTemp.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTemperatureC() + " °C"));
        vitalsColBmi.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.1f (%s)", c.getValue().calculateBmi(), c.getValue().getBmiCategory())));

        vitalsTable.setItems(vitalsHistory);
    }

    private void updateSparklines(List<PatientVitals> list) {
        sparklinesContainer.getChildren().clear();
        if (list == null || list.isEmpty()) return;

        List<Double> sysBpList = list.stream().map(v -> (double) v.getSystolicBp()).toList();
        List<Double> pulseList = list.stream().map(v -> (double) v.getPulseBpm()).toList();
        List<Double> spo2List = list.stream().map(v -> (double) v.getSpo2Percent()).toList();

        sparklinesContainer.getChildren().addAll(
            createSparklineCard("Blood Pressure (Systolic)", sysBpList, Color.web("#0F766E")),
            createSparklineCard("Heart Rate Pulse", pulseList, Color.web("#2563EB")),
            createSparklineCard("Oxygen Saturation (SpO2)", spo2List, Color.web("#16A34A"))
        );
    }

    private VBox createSparklineCard(String title, List<Double> values, Color stroke) {
        VBox card = new VBox(6);
        card.getStyleClass().add("surface-card-alt");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 14));

        Label t = new Label(title);
        t.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -color-text-secondary;");

        Sparkline sparkline = new Sparkline(values, 180, 45, stroke, stroke.deriveColor(0, 1, 1, 0.15));
        card.getChildren().addAll(t, sparkline);
        return card;
    }

    private void initPrescribingTab() {
        formColBrand.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBrandName()));
        formColGeneric.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGenericName()));
        formColType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().name()));
        formColStrength.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStrength()));
        formColPrice.setCellValueFactory(c -> new SimpleStringProperty("₹" + c.getValue().getPricePerUnit().toPlainString()));

        formularyTable.setItems(formularyResults);
        formularyResults.setAll(medicationDAO.findAll());

        formularyTable.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected != null) {
                dosageField.setText(selected.getType().getDefaultDosageHint());
                durationField.setText("5");
                instructionsField.setText("Take after meals with water");
            }
        });

        frequencyCombo.setItems(FXCollections.observableArrayList(Frequency.values()));
        frequencyCombo.setValue(Frequency.THRICE_DAILY);

        // Typo Autocorrect Search Field
        didYouMeanContainer.setVisible(false);
        didYouMeanContainer.setManaged(false);

        medSearchField.setOnSearch(query -> {
            if (query == null || query.isBlank()) {
                formularyResults.setAll(medicationDAO.findAll());
                didYouMeanContainer.setVisible(false);
                didYouMeanContainer.setManaged(false);
            } else {
                MedicationSearchService.SearchResponse response = searchService.search(query, 15);
                formularyResults.setAll(response.results());

                if (response.didYouMean().isPresent()) {
                    String suggestion = response.didYouMean().get();
                    didYouMeanLabel.setText(suggestion);
                    didYouMeanContainer.setVisible(true);
                    didYouMeanContainer.setManaged(true);
                } else {
                    didYouMeanContainer.setVisible(false);
                    didYouMeanContainer.setManaged(false);
                }
            }
        });

        // Prescription Items Table
        rxColMedName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedication() != null ? c.getValue().getMedication().getBrandName() : "Med #" + c.getValue().getMedicationId()));
        rxColDosage.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDosage()));
        rxColFrequency.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFrequency().getLabel()));
        rxColDuration.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDurationDays() + " days"));
        rxColUnits.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getTotalUnits())));
        rxColCost.setCellValueFactory(c -> new SimpleStringProperty("₹" + c.getValue().getLineCost().toPlainString()));

        rxColAction.setCellFactory(col -> new TableCell<>() {
            private final Button delBtn = new Button();

            {
                try {
                    FontIcon icon = new FontIcon("mdi2d-delete");
                    icon.setIconSize(14);
                    icon.setStyle("-fx-icon-color: -color-critical;");
                    delBtn.setGraphic(icon);
                } catch (Exception ignored) {
                    delBtn.setText("×");
                }
                delBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                delBtn.setOnAction(e -> {
                    PrescriptionItem item = getTableRow().getItem();
                    if (item != null) {
                        currentPrescription.removeItem(item);
                        prescriptionItems.setAll(currentPrescription.getItems());
                        updatePrescriptionTotalsAndSafety();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(delBtn);
                }
            }
        });

        rxItemsTable.setItems(prescriptionItems);

        addItemBtn.setOnAction(e -> handleAddMedicationItem());
        approveRxBtn.setOnAction(e -> handleApprovePrescription());
        downloadPdfBtn.setOnAction(e -> handleDownloadPdf());
    }

    @FXML
    public void applyAutocorrectSuggestion() {
        String suggestion = didYouMeanLabel.getText();
        if (suggestion != null && !suggestion.isBlank()) {
            medSearchField.setText(suggestion);
        }
    }

    private void handleAddMedicationItem() {
        if (activePatient == null) {
            Toast.show(doctorRootPane, "No active patient consultation selected.", Toast.Type.WARNING);
            return;
        }

        Medication med = formularyTable.getSelectionModel().getSelectedItem();
        if (med == null) {
            Toast.show(doctorRootPane, "Please select a medication from the formulary.", Toast.Type.WARNING);
            return;
        }

        String dosage = dosageField.getText();
        if (!med.isValidDosage(dosage)) {
            Toast.show(doctorRootPane, "Invalid dosage format: " + med.getDosageHint(), Toast.Type.ERROR);
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(durationField.getText().trim());
            if (duration <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            Toast.show(doctorRootPane, "Please enter a valid positive duration in days.", Toast.Type.ERROR);
            return;
        }

        Frequency freq = frequencyCombo.getValue();
        String instructions = instructionsField.getText() != null ? instructionsField.getText().trim() : "";

        // Polymorphic Unit & Cost Calculation
        // OOM: Polymorphism – delegates calculateUnits and calculateCost directly to Medication subclass.
        int totalUnits = med.calculateUnits(dosage, freq.getTimesPerDay(), duration);
        BigDecimal lineCost = med.calculateCost(totalUnits);

        PrescriptionItem item = new PrescriptionItem(
                null,
                null,
                med.getId(),
                dosage.trim(),
                freq,
                duration,
                instructions,
                med.getPricePerUnit(),
                totalUnits,
                lineCost
        );
        item.setMedication(med);

        // Composition: Add to prescription
        currentPrescription.addItem(item);
        prescriptionItems.setAll(currentPrescription.getItems());

        updatePrescriptionTotalsAndSafety();
        Toast.show(doctorRootPane, "Added " + med.getBrandName() + " to prescription.", Toast.Type.SUCCESS);
    }

    private void updatePrescriptionTotalsAndSafety() {
        // Update Running Total in ₹
        BigDecimal total = currentPrescription.calculateTotalCost();
        totalCostLabel.setText("₹" + total.toPlainString());

        // Run Safety Radar Screening
        alertsListContainer.getChildren().clear();
        boolean hasBlockingCritical = false;

        if (activePatient != null && !currentPrescription.getItems().isEmpty()) {
            List<PrescriptionAlert> alerts = safetyEngine.evaluate(currentPrescription, activePatient);

            for (PrescriptionAlert alert : alerts) {
                // Check if already acknowledged in currentPrescription
                for (PrescriptionAlert existing : currentPrescription.getAlerts()) {
                    if (existing.getMessage().equals(alert.getMessage()) && existing.isAcknowledged()) {
                        alert.acknowledge(existing.getOverrideNote(), existing.getOverriddenBy());
                        break;
                    }
                }

                if (alert.getLevel() == AlertLevel.CRITICAL && !alert.isAcknowledged()) {
                    hasBlockingCritical = true;
                }

                AlertCard card = new AlertCard(alert, this::promptOverride);
                alertsListContainer.getChildren().add(card);
            }

            // Sync alerts into prescription entity
            currentPrescription.clearAlerts();
            for (PrescriptionAlert a : alerts) {
                currentPrescription.addAlert(a);
            }
        }

        // High-Visibility Blocking Banner
        blockingBanner.setVisible(hasBlockingCritical);
        blockingBanner.setManaged(hasBlockingCritical);
        approveRxBtn.setDisable(hasBlockingCritical || currentPrescription.getItems().isEmpty());
    }

    private void promptOverride(PrescriptionAlert alert) {
        Optional<String> noteOpt = ConfirmDialog.promptClinicalOverride(doctorRootPane.getScene().getWindow(), alert.getMessage());
        if (noteOpt.isPresent()) {
            Doctor doc = SessionManager.getInstance().getCurrentDoctor();
            String docUser = doc != null ? doc.getUsername() : "doctor";
            alert.acknowledge(noteOpt.get(), docUser);

            Toast.show(doctorRootPane, "Critical override registered. Documented in forensic audit trail.", Toast.Type.WARNING);
            updatePrescriptionTotalsAndSafety();
        }
    }

    private void handleApprovePrescription() {
        if (activePatient == null || currentPrescription == null || currentPrescription.getItems().isEmpty()) {
            Toast.show(doctorRootPane, "Cannot approve empty prescription.", Toast.Type.WARNING);
            return;
        }

        Doctor doc = SessionManager.getInstance().getCurrentDoctor();
        try {
            Prescription approved = prescriptionService.savePrescription(currentPrescription, activePatient, doc);

            // Mark token as completed
            if (activeToken != null) {
                queueService.updateTokenStatus(activeToken.getId(), TokenStatus.COMPLETED);
            }

            reloadQueue();
            pastPrescriptions.setAll(prescriptionDAO.findByPatientId(activePatient.getId()));
            downloadPdfBtn.setDisable(false);

            Toast.show(doctorRootPane, "Prescription #" + approved.getId() + " approved and permanently persisted!", Toast.Type.SUCCESS);
        } catch (CriticalInteractionException e) {
            Toast.show(doctorRootPane, "Approval Blocked: " + e.getMessage(), Toast.Type.ERROR);
            updatePrescriptionTotalsAndSafety();
        } catch (Exception e) {
            logger.error("Prescription approval failed: {}", e.getMessage(), e);
            Toast.show(doctorRootPane, "Approval failed: " + e.getMessage(), Toast.Type.ERROR);
        }
    }

    private void handleDownloadPdf() {
        if (currentPrescription == null || currentPrescription.getId() == null) {
            Toast.show(doctorRootPane, "Please approve the prescription first before exporting PDF.", Toast.Type.WARNING);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Clinical Prescription PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("Audino_Rx_" + activePatient.getPatientId() + ".pdf");

        File file = chooser.showSaveDialog(doctorRootPane.getScene().getWindow());
        if (file != null) {
            try {
                pdfService.generatePdf(currentPrescription, latestVitals, file, true);
                Toast.show(doctorRootPane, "Saved Prescription PDF: " + file.getName(), Toast.Type.SUCCESS);
            } catch (Exception ex) {
                logger.error("Error writing PDF: {}", ex.getMessage(), ex);
                Toast.show(doctorRootPane, "PDF export failed: " + ex.getMessage(), Toast.Type.ERROR);
            }
        }
    }

    private void initHistoryTab() {
        histColDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))));
        histColDoctor.setCellValueFactory(c -> new SimpleStringProperty("Dr. " + c.getValue().getDoctorUsername()));
        histColItems.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItems().size() + " medications prescribed"));
        histColCost.setCellValueFactory(c -> new SimpleStringProperty("₹" + c.getValue().getTotalCost().toPlainString()));

        historyTable.setItems(pastPrescriptions);
    }

    @Override
    public void onQueueUpdated(List<Token> tokens) {
        Platform.runLater(this::reloadQueue);
    }
}
