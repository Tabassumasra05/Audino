package com.audino.controller;

import com.audino.dao.AccountDAO;
import com.audino.dao.PatientDAO;
import com.audino.dao.TokenDAO;
import com.audino.dao.VitalsDAO;
import com.audino.dao.jdbc.AccountDAOJdbc;
import com.audino.dao.jdbc.PatientDAOJdbc;
import com.audino.dao.jdbc.TokenDAOJdbc;
import com.audino.dao.jdbc.VitalsDAOJdbc;
import com.audino.exception.InvalidTokenStateException;
import com.audino.model.Account;
import com.audino.model.Doctor;
import com.audino.model.Gender;
import com.audino.model.Patient;
import com.audino.model.PatientAllergy;
import com.audino.model.PatientCondition;
import com.audino.model.PatientVitals;
import com.audino.model.Role;
import com.audino.model.Token;
import com.audino.model.TokenStatus;
import com.audino.observer.QueueObserver;
import com.audino.service.QueueService;
import com.audino.ui.components.Chip;
import com.audino.ui.components.ConfirmDialog;
import com.audino.ui.components.EmptyState;
import com.audino.ui.components.SearchField;
import com.audino.ui.components.StatCard;
import com.audino.ui.components.Stepper;
import com.audino.ui.components.TagInput;
import com.audino.ui.components.Toast;
import com.audino.util.ConfigurationManager;
import com.audino.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for the Receptionist & Triage Portal:
 * Handles multi-step patient registration, triage vitals recording with BMI calculation,
 * sequential token issuance, and live queue status updates.
 */
public class ReceptionistController implements QueueObserver {

    private static final Logger logger = LoggerFactory.getLogger(ReceptionistController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    @FXML private StackPane receptionistRoot;
    @FXML private GridPane receptionKpiContainer;

    // Stepper Container
    @FXML private VBox stepperContainer;
    @FXML private VBox step1Box;
    @FXML private VBox step2Box;
    @FXML private VBox step3Box;
    @FXML private VBox step4Box;
    @FXML private Label step4SummaryLabel;

    // Step 1: Demographics
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private DatePicker dobPicker;
    @FXML private ComboBox<Gender> genderCombo;
    @FXML private TextField phoneField;
    @FXML private TextArea addressArea;

    // Step 2: Clinical History (Allergies & Conditions)
    @FXML private VBox allergyTagContainer;
    @FXML private VBox conditionTagContainer;
    private TagInput allergyTagInput;
    private TagInput conditionTagInput;

    // Step 3: Vitals & BMI
    @FXML private TextField heightField;
    @FXML private TextField weightField;
    @FXML private TextField sysBpField;
    @FXML private TextField diaBpField;
    @FXML private TextField pulseField;
    @FXML private TextField tempField;
    @FXML private TextField spo2Field;
    @FXML private Label bmiDisplayLabel;
    @FXML private Label bmiCategoryLabel;

    // Wizard Controls
    @FXML private HBox wizardNavBox;
    @FXML private Button stepBackBtn;
    @FXML private Button stepNextBtn;
    @FXML private Button registerPatientBtn;

    // Queue Management
    @FXML private SearchField patientSearchField;
    @FXML private TableView<Patient> patientSearchResultTable;
    @FXML private TableColumn<Patient, String> patColId;
    @FXML private TableColumn<Patient, String> patColName;
    @FXML private TableColumn<Patient, String> patColPhone;

    @FXML private ComboBox<Account> doctorAssignCombo;
    @FXML private Button issueTokenBtn;
    @FXML private Label selectedPatientPrompt;
    @FXML private HBox selectedPatientBox;
    @FXML private Label selectedPatientNameLabel;
    @FXML private Label selectedPatientMetaLabel;

    // Live Queue Table
    @FXML private TableView<Token> queueTable;
    @FXML private TableColumn<Token, String> queueColToken;
    @FXML private TableColumn<Token, String> queueColPatient;
    @FXML private TableColumn<Token, String> queueColTime;
    @FXML private TableColumn<Token, String> queueColDoctor;
    @FXML private TableColumn<Token, String> queueColStatus;
    @FXML private TableColumn<Token, Void> queueColActions;

    private PatientDAO patientDAO;
    private TokenDAO tokenDAO;
    private VitalsDAO vitalsDAO;
    private AccountDAO accountDAO;
    private QueueService queueService;

    private Stepper stepper;
    private int currentWizardStep = 0;
    private Patient selectedPatientForToken;

    private final ObservableList<Patient> patientSearchResults = FXCollections.observableArrayList();
    private final ObservableList<Token> liveQueueList = FXCollections.observableArrayList();
    private final Map<String, String> doctorNameByUsername = new HashMap<>();

    @FXML
    public void initialize() {
        DataSource ds = ConfigurationManager.getInstance().getDataSource();
        this.patientDAO = new PatientDAOJdbc(ds);
        this.tokenDAO = new TokenDAOJdbc(ds);
        this.vitalsDAO = new VitalsDAOJdbc(ds);
        this.accountDAO = new AccountDAOJdbc(ds);
        this.queueService = new QueueService(tokenDAO, patientDAO);

        // Register as queue observer
        queueService.addObserver(this);

        initWizard();
        initQueueManagement();
        reloadQueueTable();
    }

    private void initWizard() {
        // Initialize 4-step Stepper with crisp, non-truncating labels
        stepper = new Stepper(List.of(
            new Stepper.StepInfo("Details", "Demographics"),
            new Stepper.StepInfo("Medical", "History"),
            new Stepper.StepInfo("Vitals", "Triage"),
            new Stepper.StepInfo("Token", "Queue")
        ));
        stepperContainer.getChildren().setAll(stepper);

        genderCombo.setItems(FXCollections.observableArrayList(Gender.values()));
        genderCombo.setValue(Gender.MALE);
        dobPicker.setValue(LocalDate.of(1990, 1, 1));

        // Tag inputs
        allergyTagInput = new TagInput("Type allergen (e.g. Penicillin, Sulfa) + Enter", Chip.Variant.DANGER);
        allergyTagContainer.getChildren().add(allergyTagInput);

        conditionTagInput = new TagInput("Type condition (e.g. Hypertension, Diabetes) + Enter", Chip.Variant.WARNING);
        conditionTagContainer.getChildren().add(conditionTagInput);

        // Real-time BMI Listener
        heightField.textProperty().addListener((obs, o, n) -> recalculateBmi());
        weightField.textProperty().addListener((obs, o, n) -> recalculateBmi());

        showWizardStep(0);
    }

    private void recalculateBmi() {
        try {
            double h = Double.parseDouble(heightField.getText().trim());
            double w = Double.parseDouble(weightField.getText().trim());
            if (h > 50 && w > 10) {
                double heightM = h / 100.0;
                double bmi = w / (heightM * heightM);
                bmiDisplayLabel.setText(String.format("BMI: %.1f", bmi));

                String category;
                String color;
                if (bmi < 18.5) {
                    category = "Underweight";
                    color = "#2563EB";
                } else if (bmi < 25.0) {
                    category = "Normal Weight";
                    color = "#16A34A";
                } else if (bmi < 30.0) {
                    category = "Overweight";
                    color = "#D97706";
                } else {
                    category = "Obese";
                    color = "#DC2626";
                }
                bmiCategoryLabel.setText("• " + category);
                bmiCategoryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + ";");
                return;
            }
        } catch (Exception ignored) {
        }
        bmiDisplayLabel.setText("BMI: —");
        bmiCategoryLabel.setText("");
    }

    @FXML
    public void handleNextStep() {
        if (currentWizardStep == 0) {
            String first = firstNameField.getText();
            String last = lastNameField.getText();
            String phone = phoneField.getText();
            if (first == null || first.isBlank() || last == null || last.isBlank() || phone == null || phone.isBlank()) {
                Toast.show(receptionistRoot, "First Name, Last Name, and Phone are mandatory.", Toast.Type.WARNING);
                return;
            }
        }
        showWizardStep(currentWizardStep + 1);
    }

    @FXML
    public void handleBackStep() {
        showWizardStep(currentWizardStep - 1);
    }

    @FXML
    public void handleStartNewRegistration() {
        resetWizard();
    }

    private void showWizardStep(int step) {
        this.currentWizardStep = Math.max(0, Math.min(step, 3));
        stepper.setCurrentStep(currentWizardStep);

        step1Box.setVisible(currentWizardStep == 0);
        step1Box.setManaged(currentWizardStep == 0);

        step2Box.setVisible(currentWizardStep == 1);
        step2Box.setManaged(currentWizardStep == 1);

        step3Box.setVisible(currentWizardStep == 2);
        step3Box.setManaged(currentWizardStep == 2);

        if (step4Box != null) {
            step4Box.setVisible(currentWizardStep == 3);
            step4Box.setManaged(currentWizardStep == 3);
        }

        boolean showBottomNav = currentWizardStep < 3;
        if (wizardNavBox != null) {
            wizardNavBox.setVisible(showBottomNav);
            wizardNavBox.setManaged(showBottomNav);
        }

        stepBackBtn.setVisible(currentWizardStep > 0 && currentWizardStep < 3);
        stepBackBtn.setManaged(currentWizardStep > 0 && currentWizardStep < 3);

        stepNextBtn.setVisible(currentWizardStep < 2);
        stepNextBtn.setManaged(currentWizardStep < 2);

        registerPatientBtn.setVisible(currentWizardStep == 2);
        registerPatientBtn.setManaged(currentWizardStep == 2);
    }

    @FXML
    public void handleRegisterPatient() {
        String first = firstNameField.getText().trim();
        String last = lastNameField.getText().trim();
        LocalDate dob = dobPicker.getValue();
        Gender gender = genderCombo.getValue();
        String phone = phoneField.getText().trim();
        String address = addressArea.getText() != null ? addressArea.getText().trim() : "";

        String patientId = patientDAO.getNextPatientId();

        // 1. Create Patient with Compositional History
        Patient patient = new Patient(null, patientId, first, last, dob, gender, phone, address);
        for (String a : allergyTagInput.getTags()) {
            patient.addAllergy(new PatientAllergy(a, "CRITICAL"));
        }
        for (String c : conditionTagInput.getTags()) {
            patient.addCondition(new PatientCondition(c, LocalDate.now()));
        }

        Patient saved = patientDAO.save(patient);

        // 2. Parse and save initial vitals if provided
        try {
            double h = Double.parseDouble(heightField.getText().trim());
            double w = Double.parseDouble(weightField.getText().trim());
            int sys = Integer.parseInt(sysBpField.getText().trim());
            int dia = Integer.parseInt(diaBpField.getText().trim());
            int pulse = Integer.parseInt(pulseField.getText().trim());
            double temp = Double.parseDouble(tempField.getText().trim());
            int spo2 = Integer.parseInt(spo2Field.getText().trim());

            PatientVitals vitals = new PatientVitals(null, saved.getId(), LocalDateTime.now(), h, w, sys, dia, pulse, temp, spo2);
            vitalsDAO.save(vitals);
        } catch (Exception ex) {
            logger.info("Optional initial vitals omitted or partial for patient {}", saved.getPatientId());
        }

        Toast.show(receptionistRoot, "Registered patient " + saved.getFullName() + " (" + saved.getPatientId() + ") successfully!", Toast.Type.SUCCESS);

        // Auto-select for token issuance and advance wizard to Token confirmation step
        selectPatientForToken(saved);
        if (step4SummaryLabel != null) {
            step4SummaryLabel.setText(saved.getFullName() + " (" + saved.getPatientId() + ") · Ph: " + saved.getPhone());
        }
        showWizardStep(3);
    }

    private void resetWizard() {
        firstNameField.clear();
        lastNameField.clear();
        phoneField.clear();
        addressArea.clear();
        allergyTagInput.clearTags();
        conditionTagInput.clearTags();
        heightField.clear();
        weightField.clear();
        sysBpField.clear();
        diaBpField.clear();
        pulseField.clear();
        tempField.clear();
        spo2Field.clear();
        showWizardStep(0);
    }

    private String formatDoctorLabel(Account a) {
        if (a == null) return "";
        if (a instanceof Doctor d && d.getSpecialization() != null && !d.getSpecialization().isBlank()) {
            return d.getFullName() + " \u00B7 " + d.getSpecialization();
        }
        return a.getFullName();
    }

    private String resolveDoctorFullName(String doctorUsername) {
        if (doctorUsername == null || doctorUsername.isBlank()) {
            return "Any Doctor";
        }
        String cached = doctorNameByUsername.get(doctorUsername);
        if (cached != null) {
            return cached;
        }
        return accountDAO.findByUsername(doctorUsername)
                .map(acc -> {
                    doctorNameByUsername.put(doctorUsername, acc.getFullName());
                    return acc.getFullName();
                })
                .orElse(doctorUsername);
    }

    private void initQueueManagement() {
        // Patient Search Table with EmptyState placeholder
        patColId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPatientId()));
        patColName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        patColPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));

        patientSearchResultTable.setPlaceholder(new EmptyState(
                "mdi2a-account-search",
                "No Matching Patients Found",
                "Try searching by patient full name, patient ID, or 10-digit phone number.",
                true
        ));
        patientSearchResultTable.setItems(patientSearchResults);
        patientSearchResultTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                selectPatientForToken(n);
                patientSearchResultTable.setVisible(false);
                patientSearchResultTable.setManaged(false);
            }
        });

        patientSearchField.setOnSearch(q -> {
            if (q == null || q.isBlank()) {
                patientSearchResults.clear();
                patientSearchResultTable.setVisible(false);
                patientSearchResultTable.setManaged(false);
            } else {
                patientSearchResults.setAll(patientDAO.search(q));
                patientSearchResultTable.setVisible(true);
                patientSearchResultTable.setManaged(true);
            }
        });

        // Doctor Assignment Combo & Full Name Cache
        List<Account> allAccounts = accountDAO.findAll();
        doctorNameByUsername.clear();
        for (Account acc : allAccounts) {
            if (acc.getRole() == Role.DOCTOR) {
                doctorNameByUsername.put(acc.getUsername(), acc.getFullName());
            }
        }
        List<Account> doctors = allAccounts.stream()
                .filter(a -> a.getRole() == Role.DOCTOR && a.isActive())
                .toList();

        doctorAssignCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Account a) {
                return formatDoctorLabel(a);
            }

            @Override
            public Account fromString(String string) {
                return null;
            }
        });
        doctorAssignCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    String fullText = formatDoctorLabel(item);
                    setText(fullText);
                    setTextOverrun(OverrunStyle.ELLIPSIS);
                    setTooltip(new Tooltip(fullText));
                }
            }
        });
        doctorAssignCombo.setButtonCell(new ListCell<>() {
            {
                setMinWidth(0);
                setPrefWidth(1);
                setMaxWidth(Double.MAX_VALUE);
                setTextOverrun(OverrunStyle.ELLIPSIS);
            }

            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatDoctorLabel(item));
                }
            }
        });
        doctorAssignCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                doctorAssignCombo.setTooltip(new Tooltip(formatDoctorLabel(n)));
            } else {
                doctorAssignCombo.setTooltip(null);
            }
        });
        doctorAssignCombo.setItems(FXCollections.observableArrayList(doctors));
        if (!doctors.isEmpty()) {
            doctorAssignCombo.setValue(doctors.get(0));
            doctorAssignCombo.setTooltip(new Tooltip(formatDoctorLabel(doctors.get(0))));
        }

        // Live Queue Table Columns
        queueColToken.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTokenNo()));
        queueColTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIssuedAt().format(TIME_FMT)));
        queueColPatient.setCellValueFactory(c -> {
            Optional<Patient> p = patientDAO.findById(c.getValue().getPatientId());
            return new SimpleStringProperty(p.map(Patient::getFullName).orElse("Patient #" + c.getValue().getPatientId()));
        });
        queueColDoctor.setCellValueFactory(c -> new SimpleStringProperty(resolveDoctorFullName(c.getValue().getDoctorUsername())));

        queueColStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Token t = getTableRow().getItem();
                    Chip.Variant variant = switch (t.getStatus()) {
                        case WAITING -> Chip.Variant.WARNING;
                        case IN_CONSULTATION -> Chip.Variant.PRIMARY;
                        case COMPLETED -> Chip.Variant.SUCCESS;
                        case CANCELLED -> Chip.Variant.DANGER;
                    };
                    Chip chip = new Chip(t.getStatus().name().replace('_', ' '), variant);
                    setGraphic(chip);
                }
            }
        });

        queueColActions.setCellFactory(col -> new TableCell<>() {
            private final Button callBtn = new Button("Call");
            private final Button cancelBtn = new Button("Cancel");

            {
                callBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3px 8px; -fx-background-color: -color-primary-soft; -fx-text-fill: -color-primary; -fx-font-weight: bold; -fx-cursor: hand;");
                cancelBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3px 8px; -fx-background-color: -color-critical-soft; -fx-text-fill: -color-critical; -fx-cursor: hand;");

                callBtn.setOnAction(e -> {
                    Token t = getTableRow().getItem();
                    if (t != null) {
                        try {
                            queueService.callNextPatient(t.getDoctorUsername() != null ? t.getDoctorUsername() : "doctor");
                            Toast.show(receptionistRoot, "Token " + t.getTokenNo() + " called into consultation room.", Toast.Type.INFO);
                        } catch (Exception ex) {
                            Toast.show(receptionistRoot, "Could not call token: " + ex.getMessage(), Toast.Type.ERROR);
                        }
                    }
                });

                cancelBtn.setOnAction(e -> {
                    Token t = getTableRow().getItem();
                    if (t != null) {
                        try {
                            queueService.cancelToken(t.getId(), "reception", "Cancelled by front desk");
                            Toast.show(receptionistRoot, "Token " + t.getTokenNo() + " cancelled.", Toast.Type.WARNING);
                        } catch (InvalidTokenStateException ex) {
                            Toast.show(receptionistRoot, "State violation: " + ex.getMessage(), Toast.Type.ERROR);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Token t = getTableRow().getItem();
                    if (t.getStatus() == TokenStatus.WAITING) {
                        HBox box = new HBox(6, callBtn, cancelBtn);
                        box.setAlignment(Pos.CENTER);
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        queueTable.setPlaceholder(new EmptyState(
                "mdi2t-ticket-outline",
                "No Outpatient Tokens Issued Today",
                "Search for a registered patient or complete the intake wizard on the left to issue a token."
        ));
        queueTable.setItems(liveQueueList);
    }

    private void selectPatientForToken(Patient p) {
        this.selectedPatientForToken = p;
        if (p != null) {
            selectedPatientNameLabel.setText(p.getFullName() + " (" + p.getPatientId() + ")");
            selectedPatientMetaLabel.setText(p.getAge() + " yrs · " + p.getGender() + " · Ph: " + p.getPhone());
            selectedPatientBox.setVisible(true);
            selectedPatientBox.setManaged(true);
            selectedPatientPrompt.setVisible(false);
            selectedPatientPrompt.setManaged(false);
            issueTokenBtn.setDisable(false);
        } else {
            selectedPatientBox.setVisible(false);
            selectedPatientBox.setManaged(false);
            selectedPatientPrompt.setVisible(true);
            selectedPatientPrompt.setManaged(true);
            issueTokenBtn.setDisable(true);
        }
    }

    @FXML
    public void handleClearSelectedPatient() {
        selectPatientForToken(null);
        patientSearchField.setText("");
        patientSearchResults.clear();
        patientSearchResultTable.setVisible(false);
        patientSearchResultTable.setManaged(false);
    }

    @FXML
    public void handleIssueToken() {
        if (selectedPatientForToken == null) {
            Toast.show(receptionistRoot, "Please search and select a patient first.", Toast.Type.WARNING);
            return;
        }

        Account doc = doctorAssignCombo.getValue();
        String docUser = doc != null ? doc.getUsername() : "doctor";

        Token token = queueService.issueToken(selectedPatientForToken.getId(), docUser);
        Toast.show(receptionistRoot, "Issued Token #" + token.getTokenNo() + " for " + selectedPatientForToken.getFullName(), Toast.Type.SUCCESS);
        reloadQueueTable();
        if (currentWizardStep == 3) {
            resetWizard();
        }
    }

    private void reloadQueueTable() {
        liveQueueList.setAll(tokenDAO.findTodayTokens());
        updateQueueSummary();
    }

    private void updateQueueSummary() {
        if (receptionKpiContainer == null) return;
        List<Token> all = tokenDAO.findTodayTokens();
        long total = all.size();
        long waiting = all.stream().filter(t -> t.getStatus() == TokenStatus.WAITING).count();
        long inConsult = all.stream().filter(t -> t.getStatus() == TokenStatus.IN_CONSULTATION).count();
        long completed = all.stream().filter(t -> t.getStatus() == TokenStatus.COMPLETED).count();

        StatCard.populateRow(receptionKpiContainer,
            new StatCard("Today's Registrations", String.valueOf(total), "Issued outpatient tokens", "mdi2t-ticket-confirmation", "primary-soft"),
            new StatCard("Waiting in OPD", String.valueOf(waiting), "Awaiting doctor consultation", "mdi2a-account-clock", "warning-soft"),
            new StatCard("In Consultation", String.valueOf(inConsult), "Currently with physician", "mdi2s-stethoscope", "info-soft"),
            new StatCard("Completed Visits", String.valueOf(completed), "Prescription issued", "mdi2c-check-circle", "success-soft")
        );
    }

    @Override
    public void onQueueUpdated(List<Token> tokens) {
        // Synchronize on JavaFX UI Application Thread
        Platform.runLater(this::reloadQueueTable);
    }
}
