package com.audino.controller;

import com.audino.dao.PatientDAO;
import com.audino.dao.TokenDAO;
import com.audino.dao.jdbc.PatientDAOJdbc;
import com.audino.dao.jdbc.TokenDAOJdbc;
import com.audino.model.Doctor;
import com.audino.model.Patient;
import com.audino.model.Token;
import com.audino.model.TokenStatus;
import com.audino.service.QueueService;
import com.audino.ui.components.Chip;
import com.audino.ui.components.StatCard;
import com.audino.ui.navigation.NavigationService;
import com.audino.util.ConfigurationManager;
import com.audino.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for Doctor Home Dashboard: greeting, queue statistics, next patient triage card,
 * and outpatient roster management.
 */
public class DoctorDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorDashboardController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label doctorGreetingLabel;
    @FXML private Label doctorSubLabel;
    @FXML private Button quickConsultBtn;
    @FXML private HBox kpiContainer;

    @FXML private Label nextTokenLabel;
    @FXML private Label nextPatientNameLabel;
    @FXML private Label nextPatientDetailsLabel;
    @FXML private Label nextWaitTimeLabel;
    @FXML private Button callNextActionBtn;

    @FXML private Button refreshQueueBtn;
    @FXML private TableView<Token> queueTableView;
    @FXML private TableColumn<Token, String> colToken;
    @FXML private TableColumn<Token, String> colTime;
    @FXML private TableColumn<Token, String> colPatient;
    @FXML private TableColumn<Token, String> colAgeGender;
    @FXML private TableColumn<Token, String> colStatus;
    @FXML private TableColumn<Token, Void> colAction;

    private TokenDAO tokenDAO;
    private PatientDAO patientDAO;
    private QueueService queueService;

    private final ObservableList<Token> tokenList = FXCollections.observableArrayList();
    private Token nextWaitingToken;

    @FXML
    public void initialize() {
        DataSource ds = ConfigurationManager.getInstance().getDataSource();
        this.tokenDAO = new TokenDAOJdbc(ds);
        this.patientDAO = new PatientDAOJdbc(ds);
        this.queueService = new QueueService(tokenDAO, patientDAO);

        Doctor doc = SessionManager.getInstance().getCurrentDoctor();
        if (doc != null) {
            doctorGreetingLabel.setText("Welcome, Dr. " + doc.getFullName().replaceFirst("(?i)^Dr\\.\\s*", ""));
            doctorSubLabel.setText(doc.getSpecialization() + " | Medical Reg: " + doc.getRegistrationNumber());
        }

        initTable();
        loadDashboardData();

        quickConsultBtn.setOnAction(e -> {
            NavigationService.getInstance().navigateTo("/fxml/doctor_consultation.fxml", "Doctor Consultation");
        });

        callNextActionBtn.setOnAction(e -> handleCallNext());
        refreshQueueBtn.setOnAction(e -> loadDashboardData());
    }

    private void initTable() {
        colToken.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTokenNo()));
        colTime.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getIssuedAt() != null ? c.getValue().getIssuedAt().format(TIME_FMT) : "—"
        ));
        colPatient.setCellValueFactory(c -> {
            Optional<Patient> p = patientDAO.findById(c.getValue().getPatientId());
            return new SimpleStringProperty(p.map(Patient::getFullName).orElse("Patient #" + c.getValue().getPatientId()));
        });
        colAgeGender.setCellValueFactory(c -> {
            Optional<Patient> p = patientDAO.findById(c.getValue().getPatientId());
            return new SimpleStringProperty(p.map(pat -> pat.getAge() + " / " + pat.getGender()).orElse("—"));
        });
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Token t = getTableRow().getItem();
                    Chip.Variant v = switch (t.getStatus()) {
                        case WAITING -> Chip.Variant.WARNING;
                        case IN_CONSULTATION -> Chip.Variant.PRIMARY;
                        case COMPLETED -> Chip.Variant.SUCCESS;
                        case CANCELLED -> Chip.Variant.DANGER;
                    };
                    Chip chip = new Chip(t.getStatus().name().replace('_', ' '), v);
                    setGraphic(chip);
                }
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button consultBtn = new Button("Consult");
            {
                consultBtn.setStyle("-fx-background-color: -color-primary; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 6px; -fx-padding: 4px 10px; -fx-cursor: hand;");
                consultBtn.setOnAction(e -> {
                    Token t = getTableRow().getItem();
                    if (t != null) {
                        DoctorConsultationController.setInitialToken(t);
                        NavigationService.getInstance().navigateTo("/fxml/doctor_consultation.fxml", "Doctor Consultation");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(consultBtn);
                }
            }
        });

        queueTableView.setItems(tokenList);
    }

    private void loadDashboardData() {
        Doctor doc = SessionManager.getInstance().getCurrentDoctor();
        String docUser = doc != null ? doc.getUsername() : "doctor";

        List<Token> allTokens = tokenDAO.findTodayTokensByDoctor(docUser);
        tokenList.setAll(allTokens);

        long waitingCount = allTokens.stream().filter(t -> t.getStatus() == TokenStatus.WAITING).count();
        long inConsultationCount = allTokens.stream().filter(t -> t.getStatus() == TokenStatus.IN_CONSULTATION).count();
        long completedCount = allTokens.stream().filter(t -> t.getStatus() == TokenStatus.COMPLETED).count();
        long totalCount = allTokens.size();

        kpiContainer.getChildren().setAll(
            new StatCard("Waiting Patients", String.valueOf(waitingCount), "In OPD waiting area", "mdi2a-account-clock", "warning-soft"),
            new StatCard("In Consultation", String.valueOf(inConsultationCount), "Active consultation room", "mdi2s-stethoscope", "primary-soft"),
            new StatCard("Completed Today", String.valueOf(completedCount), "Prescriptions dispensed", "mdi2c-check-circle", "success-soft"),
            new StatCard("Total Tokens", String.valueOf(totalCount), "All registrations today", "mdi2t-ticket-confirmation", "info-soft")
        );

        // Next waiting token
        nextWaitingToken = allTokens.stream()
                .filter(t -> t.getStatus() == TokenStatus.WAITING)
                .findFirst()
                .orElse(null);

        if (nextWaitingToken != null) {
            nextTokenLabel.setText(nextWaitingToken.getTokenNo());
            Optional<Patient> pOpt = patientDAO.findById(nextWaitingToken.getPatientId());
            if (pOpt.isPresent()) {
                Patient p = pOpt.get();
                nextPatientNameLabel.setText(p.getFullName() + ", " + p.getAge() + " " + p.getGender());
                nextPatientDetailsLabel.setText("Phone: " + p.getPhone() + " | Address: " + (p.getAddress() != null ? p.getAddress() : "Local OPD"));
            } else {
                nextPatientNameLabel.setText("Patient #" + nextWaitingToken.getPatientId());
                nextPatientDetailsLabel.setText("Details pending triage");
            }

            if (nextWaitingToken.getIssuedAt() != null) {
                long minutes = Duration.between(nextWaitingToken.getIssuedAt(), LocalDateTime.now()).toMinutes();
                nextWaitTimeLabel.setText(Math.max(1, minutes) + " mins");
            } else {
                nextWaitTimeLabel.setText("Just now");
            }

            callNextActionBtn.setDisable(false);
        } else {
            nextTokenLabel.setText("—");
            nextPatientNameLabel.setText("No Patients Currently Waiting");
            nextPatientDetailsLabel.setText("All assigned consultation tokens for today have been completed.");
            nextWaitTimeLabel.setText("0 mins");
            callNextActionBtn.setDisable(true);
        }
    }

    private void handleCallNext() {
        Doctor doc = SessionManager.getInstance().getCurrentDoctor();
        String docUser = doc != null ? doc.getUsername() : "doctor";
        try {
            Optional<Token> nextOpt = queueService.callNextPatient(docUser);
            if (nextOpt.isPresent()) {
                Token t = nextOpt.get();
                DoctorConsultationController.setInitialToken(t);
            }
            NavigationService.getInstance().navigateTo("/fxml/doctor_consultation.fxml", "Doctor Consultation");
        } catch (Exception e) {
            logger.error("Error calling next patient: {}", e.getMessage(), e);
            NavigationService.getInstance().navigateTo("/fxml/doctor_consultation.fxml", "Doctor Consultation");
        }
    }
}
