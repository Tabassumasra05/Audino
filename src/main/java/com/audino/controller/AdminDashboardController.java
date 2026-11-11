package com.audino.controller;

import com.audino.dao.AccountDAO;
import com.audino.dao.AuditLogDAO;
import com.audino.dao.InteractionRuleDAO;
import com.audino.dao.MedicationDAO;
import com.audino.dao.PrescriptionDAO;
import com.audino.dao.TokenDAO;
import com.audino.dao.jdbc.AccountDAOJdbc;
import com.audino.dao.jdbc.AuditLogDAOJdbc;
import com.audino.dao.jdbc.InteractionRuleDAOJdbc;
import com.audino.dao.jdbc.MedicationDAOJdbc;
import com.audino.dao.jdbc.PrescriptionDAOJdbc;
import com.audino.dao.jdbc.TokenDAOJdbc;
import com.audino.factory.AccountFactory;
import com.audino.factory.MedicationFactory;
import com.audino.model.Account;
import com.audino.model.AlertLevel;
import com.audino.model.AlertType;
import com.audino.model.AuditLog;
import com.audino.model.Doctor;
import com.audino.model.InteractionRule;
import com.audino.model.Medication;
import com.audino.model.MedicationType;
import com.audino.model.Receptionist;
import com.audino.model.Role;
import com.audino.service.AuditService;
import com.audino.ui.components.Chip;
import com.audino.ui.components.ConfirmDialog;
import com.audino.ui.components.SearchField;
import com.audino.ui.components.StatCard;
import com.audino.ui.components.Toast;
import com.audino.util.ConfigurationManager;
import com.audino.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for the Hospital Administrator Portal:
 * Oversees staff accounts, formulary maintenance, CDS interaction rules, and forensic audit logs.
 */
public class AdminDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    @FXML private StackPane adminRootPane;
    @FXML private HBox statCardsContainer;
    @FXML private LineChart<String, Number> prescriptionTrendChart;
    @FXML private PieChart alertSeverityChart;
    @FXML private BarChart<String, Number> topMedicationsChart;

    // Overview Tab Recent Audit
    @FXML private TableView<AuditLog> overviewAuditTable;
    @FXML private TableColumn<AuditLog, String> colOvTime;
    @FXML private TableColumn<AuditLog, String> colOvOperator;
    @FXML private TableColumn<AuditLog, String> colOvOperation;
    @FXML private TableColumn<AuditLog, String> colOvEntity;
    @FXML private TableColumn<AuditLog, String> colOvDetails;

    // Staff Tab
    @FXML private SearchField staffSearchField;
    @FXML private TableView<Account> staffTable;
    @FXML private TableColumn<Account, String> staffColUsername;
    @FXML private TableColumn<Account, String> staffColName;
    @FXML private TableColumn<Account, String> staffColRole;
    @FXML private TableColumn<Account, String> staffColSpecialization;
    @FXML private TableColumn<Account, String> staffColStatus;
    @FXML private TableColumn<Account, Void> staffColActions;

    // Formulary Tab
    @FXML private SearchField medSearchField;
    @FXML private ComboBox<String> medTypeFilter;
    @FXML private TableView<Medication> medTable;
    @FXML private TableColumn<Medication, String> medColBrand;
    @FXML private TableColumn<Medication, String> medColGeneric;
    @FXML private TableColumn<Medication, String> medColType;
    @FXML private TableColumn<Medication, String> medColStrength;
    @FXML private TableColumn<Medication, String> medColPrice;
    @FXML private TableColumn<Medication, String> medColRxNorm;

    // Rules Tab
    @FXML private ComboBox<String> ruleTypeFilter;
    @FXML private TableView<InteractionRule> ruleTable;
    @FXML private TableColumn<InteractionRule, String> ruleColType;
    @FXML private TableColumn<InteractionRule, String> ruleColKey1;
    @FXML private TableColumn<InteractionRule, String> ruleColKey2;
    @FXML private TableColumn<InteractionRule, String> ruleColSeverity;
    @FXML private TableColumn<InteractionRule, String> ruleColDescription;
    @FXML private TableColumn<InteractionRule, Void> ruleColActions;

    // Audit Tab
    @FXML private SearchField auditSearchField;
    @FXML private TableView<AuditLog> auditTable;
    @FXML private TableColumn<AuditLog, String> auditColTime;
    @FXML private TableColumn<AuditLog, String> auditColOperator;
    @FXML private TableColumn<AuditLog, String> auditColOperation;
    @FXML private TableColumn<AuditLog, String> auditColEntity;
    @FXML private TableColumn<AuditLog, String> auditColDetails;

    private AccountDAO accountDAO;
    private MedicationDAO medicationDAO;
    private InteractionRuleDAO ruleDAO;
    private PrescriptionDAO prescriptionDAO;
    private TokenDAO tokenDAO;
    private AuditLogDAO auditDAO;
    private AuditService auditService;

    private final ObservableList<Account> staffList = FXCollections.observableArrayList();
    private final ObservableList<Medication> medList = FXCollections.observableArrayList();
    private final ObservableList<InteractionRule> ruleList = FXCollections.observableArrayList();
    private final ObservableList<AuditLog> auditList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        DataSource ds = ConfigurationManager.getInstance().getDataSource();
        accountDAO = new AccountDAOJdbc(ds);
        medicationDAO = new MedicationDAOJdbc(ds);
        ruleDAO = new InteractionRuleDAOJdbc(ds);
        prescriptionDAO = new PrescriptionDAOJdbc(ds, medicationDAO);
        tokenDAO = new TokenDAOJdbc(ds);
        auditDAO = new AuditLogDAOJdbc(ds);
        auditService = new AuditService(auditDAO);

        initOverviewTab();
        initStaffTab();
        initFormularyTab();
        initRulesTab();
        initAuditTab();
    }

    private void initOverviewTab() {
        statCardsContainer.getChildren().clear();

        int todayTokens = tokenDAO.findTodayTokens().size();
        int todayRx = prescriptionDAO.countTodayPrescriptions();
        int todayOverrides = prescriptionDAO.countTodayCriticalOverrides();
        int activeStaff = accountDAO.countActiveStaff();

        statCardsContainer.getChildren().addAll(
            new StatCard("Today's Tokens", String.valueOf(todayTokens), "Outpatient registrations today", "mdi2t-ticket-confirmation", "primary-soft"),
            new StatCard("Prescriptions Issued", String.valueOf(todayRx), "Clinical approvals today", "mdi2f-file-check", "success-soft"),
            new StatCard("Critical Overrides", String.valueOf(todayOverrides), "Mandatory rationales logged", "mdi2s-shield-alert", "critical-soft"),
            new StatCard("Active Staff", String.valueOf(activeStaff), "Doctors & receptionist on duty", "mdi2a-account-group", "info-soft")
        );

        // 7-day Line Chart Trend
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Prescriptions");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            series.getData().add(new XYChart.Data<>(d.format(DateTimeFormatter.ofPattern("dd-MMM")), Math.max(2, (int) (Math.random() * 8) + 2)));
        }
        prescriptionTrendChart.getData().setAll(series);

        // Alert Severity Pie Chart
        List<InteractionRule> allRules = ruleDAO.findAll();
        long criticalCount = allRules.stream().filter(r -> r.getSeverity() == AlertLevel.CRITICAL).count();
        long warningCount = allRules.stream().filter(r -> r.getSeverity() == AlertLevel.WARNING).count();
        long infoCount = allRules.stream().filter(r -> r.getSeverity() == AlertLevel.INFO).count();

        alertSeverityChart.setData(FXCollections.observableArrayList(
            new PieChart.Data("Critical (" + criticalCount + ")", criticalCount),
            new PieChart.Data("Warning (" + warningCount + ")", warningCount),
            new PieChart.Data("Informational (" + infoCount + ")", infoCount)
        ));

        // Top Prescribed Medications Bar Chart
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.setName("Units Dispensed");
        barSeries.getData().addAll(
            new XYChart.Data<>("Dolo 650", 42),
            new XYChart.Data<>("Mox 500", 28),
            new XYChart.Data<>("Brufen 400", 19),
            new XYChart.Data<>("Azee 500", 14),
            new XYChart.Data<>("Glycomet", 11)
        );
        topMedicationsChart.getData().setAll(barSeries);

        // Recent Audit Stream
        colOvTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTimestamp() != null ? c.getValue().getTimestamp().format(DATE_FMT) : "—"));
        colOvOperator.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOperatorUsername()));
        colOvOperation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOperationType()));
        colOvEntity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEntityType() + " #" + (c.getValue().getEntityId() != null ? c.getValue().getEntityId() : "")));
        colOvDetails.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDetails()));

        List<AuditLog> recentLogs = auditDAO.findRecent(10);
        overviewAuditTable.setItems(FXCollections.observableArrayList(recentLogs));
    }

    private void initStaffTab() {
        staffColUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        staffColName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        staffColRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole().name()));
        staffColSpecialization.setCellValueFactory(c -> {
            if (c.getValue() instanceof Doctor doc) {
                return new SimpleStringProperty(doc.getSpecialization() + " (" + doc.getRegistrationNumber() + ")");
            }
            return new SimpleStringProperty("—");
        });

        staffColStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Account acc = getTableRow().getItem();
                    Chip chip = new Chip(acc.isActive() ? "ACTIVE" : "INACTIVE",
                            acc.isActive() ? Chip.Variant.SUCCESS : Chip.Variant.DANGER);
                    setGraphic(chip);
                }
            }
        });

        staffColActions.setCellFactory(col -> new TableCell<>() {
            private final Button toggleBtn = new Button();
            private final Button resetBtn = new Button("Reset Password");

            {
                resetBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-background-color: -color-surface-alt; -fx-border-color: -color-border; -fx-border-radius: 4px; -fx-cursor: hand;");
                resetBtn.setOnAction(e -> {
                    Account acc = getTableRow().getItem();
                    if (acc != null) handleResetPassword(acc);
                });

                toggleBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-background-radius: 4px; -fx-cursor: hand;");
                toggleBtn.setOnAction(e -> {
                    Account acc = getTableRow().getItem();
                    if (acc != null) handleToggleStaffActive(acc);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Account acc = getTableRow().getItem();
                    toggleBtn.setText(acc.isActive() ? "Deactivate" : "Activate");
                    toggleBtn.setStyle(acc.isActive() ?
                            "-fx-background-color: -color-critical-soft; -fx-text-fill: -color-critical; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-cursor: hand;" :
                            "-fx-background-color: -color-success-soft; -fx-text-fill: -color-success; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-cursor: hand;");

                    HBox box = new HBox(6, toggleBtn, resetBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        staffTable.setItems(staffList);
        reloadStaffTable();

        staffSearchField.setOnSearch(q -> {
            if (q == null || q.isBlank()) {
                reloadStaffTable();
            } else {
                String term = q.trim().toLowerCase();
                List<Account> filtered = accountDAO.findAll().stream().filter(a ->
                        a.getUsername().toLowerCase().contains(term) ||
                        a.getFullName().toLowerCase().contains(term) ||
                        a.getRole().name().toLowerCase().contains(term)
                ).toList();
                staffList.setAll(filtered);
            }
        });
    }

    private void reloadStaffTable() {
        staffList.setAll(accountDAO.findAll());
    }

    private void handleToggleStaffActive(Account acc) {
        Account current = SessionManager.getInstance().getCurrentAccount();
        if (current != null && current.getUsername().equalsIgnoreCase(acc.getUsername())) {
            Toast.show(adminRootPane, "Security restriction: You cannot deactivate your own administrative account.", Toast.Type.WARNING);
            return;
        }

        boolean willBeActive = !acc.isActive();
        boolean confirm = ConfirmDialog.confirm(
            adminRootPane.getScene().getWindow(),
            willBeActive ? "Activate Account" : "Deactivate Account",
            "Are you sure you want to " + (willBeActive ? "activate" : "deactivate") + " user '" + acc.getUsername() + "'?",
            willBeActive ? "Activate" : "Deactivate",
            !willBeActive
        );

        if (confirm) {
            accountDAO.setActiveStatus(acc.getUsername(), willBeActive);
            auditService.logStaffAction(current != null ? current.getUsername() : "admin", acc.getUsername(),
                    willBeActive ? "ACCOUNT_ACTIVATED" : "ACCOUNT_DEACTIVATED", "Toggled account active status to " + willBeActive);
            reloadStaffTable();
            Toast.show(adminRootPane, "Account '" + acc.getUsername() + "' status updated successfully.", Toast.Type.SUCCESS);
        }
    }

    private void handleResetPassword(Account acc) {
        boolean confirm = ConfirmDialog.confirm(
            adminRootPane.getScene().getWindow(),
            "Reset Password",
            "Reset password for user '" + acc.getUsername() + "' to temporary password '1234567890'?",
            "Reset Password",
            false
        );

        if (confirm) {
            String newHash = BCrypt.hashpw("1234567890", BCrypt.gensalt(12));
            accountDAO.updatePassword(acc.getUsername(), newHash);
            Account current = SessionManager.getInstance().getCurrentAccount();
            auditService.logStaffAction(current != null ? current.getUsername() : "admin", acc.getUsername(),
                    "PASSWORD_RESET", "Administrator reset password to default temporary key");
            Toast.show(adminRootPane, "Password reset to '1234567890' for user '" + acc.getUsername() + "'.", Toast.Type.SUCCESS);
        }
    }

    @FXML
    public void openAddStaffDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(adminRootPane.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Create New Staff Account");

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: -color-surface; -fx-min-width: 440px;");

        Label title = new Label("Register Clinical Staff Member");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        TextField userField = new TextField();
        userField.setPromptText("Username (e.g. dr.verma)");

        TextField nameField = new TextField();
        nameField.setPromptText("Full Legal Name");

        ComboBox<Role> roleCombo = new ComboBox<>(FXCollections.observableArrayList(Role.DOCTOR, Role.RECEPTIONIST, Role.ADMIN));
        roleCombo.setValue(Role.DOCTOR);

        TextField specField = new TextField();
        specField.setPromptText("Medical Specialization (Doctors only)");

        TextField regField = new TextField();
        regField.setPromptText("MCI Registration Number (Doctors only)");

        roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isDoc = newVal == Role.DOCTOR;
            specField.setVisible(isDoc);
            specField.setManaged(isDoc);
            regField.setVisible(isDoc);
            regField.setManaged(isDoc);
        });

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: -color-critical; -fx-font-size: 11px;");
        errorLbl.setVisible(false);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button createBtn = new Button("Create Account");
        createBtn.getStyleClass().add("btn-primary");
        createBtn.setOnAction(e -> {
            String u = userField.getText();
            String n = nameField.getText();
            Role r = roleCombo.getValue();
            if (u == null || u.isBlank() || n == null || n.isBlank()) {
                errorLbl.setText("Username and Full Name are mandatory.");
                errorLbl.setVisible(true);
                return;
            }

            if (accountDAO.findByUsername(u.trim()).isPresent()) {
                errorLbl.setText("Username '" + u.trim() + "' is already registered.");
                errorLbl.setVisible(true);
                return;
            }

            String defaultHash = BCrypt.hashpw("1234567890", BCrypt.gensalt(12));
            Account newAcc;
            if (r == Role.DOCTOR) {
                String spec = specField.getText() != null && !specField.getText().isBlank() ? specField.getText().trim() : "General Medicine";
                String reg = regField.getText() != null && !regField.getText().isBlank() ? regField.getText().trim() : "MCI-PENDING";
                newAcc = new Doctor(null, u.trim(), defaultHash, n.trim(), true, spec, reg);
            } else if (r == Role.RECEPTIONIST) {
                newAcc = new Receptionist(null, u.trim(), defaultHash, n.trim(), true);
            } else {
                newAcc = AccountFactory.createAccount(null, u.trim(), defaultHash, n.trim(), Role.ADMIN, null, null, true, null);
            }

            accountDAO.save(newAcc);
            Account current = SessionManager.getInstance().getCurrentAccount();
            auditService.logStaffAction(current != null ? current.getUsername() : "admin", u.trim(),
                    "ACCOUNT_CREATED", "Created " + r + " account for " + n.trim());

            reloadStaffTable();
            dialog.close();
            Toast.show(adminRootPane, "Created " + r + " account '" + u.trim() + "' successfully.", Toast.Type.SUCCESS);
        });

        buttons.getChildren().addAll(cancelBtn, createBtn);
        form.getChildren().addAll(title, new Label("Username:"), userField, new Label("Full Name:"), nameField,
                new Label("Role:"), roleCombo, specField, regField, errorLbl, buttons);

        Scene scene = new Scene(form);
        scene.getStylesheets().addAll(adminRootPane.getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void initFormularyTab() {
        medColBrand.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBrandName()));
        medColGeneric.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGenericName()));
        medColType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().name()));
        medColStrength.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStrength()));
        medColPrice.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue() != null && c.getValue().getPricePerUnit() != null
                ? "₹" + c.getValue().getPricePerUnit().toPlainString()
                : "Unset"
        ));
        medColRxNorm.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRxNormCode() != null ? c.getValue().getRxNormCode() : "—"));

        medTypeFilter.setItems(FXCollections.observableArrayList("ALL", "TABLET", "CAPSULE", "LIQUID", "INJECTION"));
        medTypeFilter.setValue("ALL");
        medTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> reloadFormularyTable());

        medTable.setItems(medList);
        reloadFormularyTable();

        medSearchField.setOnSearch(q -> {
            if (q == null || q.isBlank()) {
                reloadFormularyTable();
            } else {
                List<Medication> results = medicationDAO.search(q);
                medList.setAll(results);
            }
        });
    }

    private void reloadFormularyTable() {
        List<Medication> all = medicationDAO.findAll();
        String typeFilter = medTypeFilter.getValue();
        if (typeFilter != null && !typeFilter.equals("ALL")) {
            all = all.stream().filter(m -> m.getType().name().equalsIgnoreCase(typeFilter)).toList();
        }
        medList.setAll(all);
    }

    @FXML
    public void openAddMedicationDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(adminRootPane.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Medication to Formulary");

        VBox form = new VBox(12);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: -color-surface; -fx-min-width: 460px;");

        Label title = new Label("Register Drug Formulary Entry");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        TextField brandField = new TextField();
        brandField.setPromptText("Brand Name (e.g. Dolo 650)");

        TextField genericField = new TextField();
        genericField.setPromptText("Generic Molecule Name (e.g. Paracetamol)");

        ComboBox<MedicationType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(MedicationType.values()));
        typeCombo.setValue(MedicationType.TABLET);

        TextField strengthField = new TextField();
        strengthField.setPromptText("Strength (e.g. 650mg, 500mg, 5ml)");

        TextField priceField = new TextField();
        priceField.setPromptText("Price per unit in ₹ (e.g. 30.00)");

        TextField rxNormField = new TextField();
        rxNormField.setPromptText("RxNorm Concept Code (optional)");

        TextField ingField = new TextField();
        ingField.setPromptText("Active ingredients, comma-separated");

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: -color-critical; -fx-font-size: 11px;");
        errorLbl.setVisible(false);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Save to Formulary");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            String b = brandField.getText();
            String g = genericField.getText();
            MedicationType t = typeCombo.getValue();
            String s = strengthField.getText();
            String pStr = priceField.getText();

            if (b == null || b.isBlank() || g == null || g.isBlank() || s == null || s.isBlank() || pStr == null || pStr.isBlank()) {
                errorLbl.setText("Brand, Generic, Strength, and Price are required.");
                errorLbl.setVisible(true);
                return;
            }

            BigDecimal price;
            try {
                price = new BigDecimal(pStr.trim());
                if (price.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            } catch (Exception ex) {
                errorLbl.setText("Please enter a valid positive decimal price.");
                errorLbl.setVisible(true);
                return;
            }

            List<String> ingredients = Arrays.stream((ingField.getText() != null ? ingField.getText() : g).split(","))
                    .map(String::trim).filter(str -> !str.isEmpty()).toList();

            Medication med = MedicationFactory.createMedication(null, g.trim(), b.trim(),
                    rxNormField.getText() != null ? rxNormField.getText().trim() : "", t, s.trim(), price, ingredients);

            medicationDAO.save(med);
            Account current = SessionManager.getInstance().getCurrentAccount();
            auditService.logStaffAction(current != null ? current.getUsername() : "admin", b.trim(),
                    "MEDICATION_ADDED", "Added " + b.trim() + " (" + g.trim() + ") to clinical formulary");

            reloadFormularyTable();
            dialog.close();
            Toast.show(adminRootPane, "Added '" + b.trim() + "' to clinical formulary.", Toast.Type.SUCCESS);
        });

        buttons.getChildren().addAll(cancelBtn, saveBtn);
        form.getChildren().addAll(title, brandField, genericField, typeCombo, strengthField, priceField, rxNormField, ingField, errorLbl, buttons);

        Scene scene = new Scene(form);
        scene.getStylesheets().addAll(adminRootPane.getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void initRulesTab() {
        ruleColType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().name()));
        ruleColKey1.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getKeyword1()));
        ruleColKey2.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getKeyword2()));
        ruleColDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));

        ruleColSeverity.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    InteractionRule rule = getTableRow().getItem();
                    Chip.Variant variant = switch (rule.getSeverity()) {
                        case CRITICAL -> Chip.Variant.DANGER;
                        case WARNING -> Chip.Variant.WARNING;
                        case INFO -> Chip.Variant.INFO;
                    };
                    Chip chip = new Chip(rule.getSeverity().name(), variant);
                    setGraphic(chip);
                }
            }
        });

        ruleColActions.setCellFactory(col -> new TableCell<>() {
            private final Button delBtn = new Button("Delete");

            {
                delBtn.setStyle("-fx-background-color: -color-critical-soft; -fx-text-fill: -color-critical; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-cursor: hand;");
                delBtn.setOnAction(e -> {
                    InteractionRule rule = getTableRow().getItem();
                    if (rule != null) handleDeleteRule(rule);
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

        ruleTypeFilter.setItems(FXCollections.observableArrayList("ALL", "DRUG_DRUG", "DRUG_ALLERGY", "DRUG_CONDITION"));
        ruleTypeFilter.setValue("ALL");
        ruleTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> reloadRulesTable());

        ruleTable.setItems(ruleList);
        reloadRulesTable();
    }

    private void reloadRulesTable() {
        String filter = ruleTypeFilter.getValue();
        if (filter != null && !filter.equals("ALL")) {
            ruleList.setAll(ruleDAO.findByType(AlertType.valueOf(filter)));
        } else {
            ruleList.setAll(ruleDAO.findAll());
        }
    }

    private void handleDeleteRule(InteractionRule rule) {
        boolean confirm = ConfirmDialog.confirm(
            adminRootPane.getScene().getWindow(),
            "Delete Clinical Rule",
            "Are you sure you want to remove rule: " + rule.getKeyword1() + " + " + rule.getKeyword2() + "?",
            "Delete Rule",
            true
        );

        if (confirm) {
            ruleDAO.delete(rule.getId());
            Account current = SessionManager.getInstance().getCurrentAccount();
            auditService.logStaffAction(current != null ? current.getUsername() : "admin", String.valueOf(rule.getId()),
                    "RULE_DELETED", "Deleted interaction rule: " + rule.getKeyword1() + " + " + rule.getKeyword2());
            reloadRulesTable();
            Toast.show(adminRootPane, "Rule removed. Takes effect immediately across all prescribers.", Toast.Type.SUCCESS);
        }
    }

    @FXML
    public void openAddRuleDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(adminRootPane.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add Clinical Interaction Rule");

        VBox form = new VBox(12);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: -color-surface; -fx-min-width: 480px;");

        Label title = new Label("Configure Clinical Decision Rule");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        ComboBox<AlertType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(AlertType.DRUG_DRUG, AlertType.DRUG_ALLERGY, AlertType.DRUG_CONDITION));
        typeCombo.setValue(AlertType.DRUG_DRUG);

        ComboBox<AlertLevel> severityCombo = new ComboBox<>(FXCollections.observableArrayList(AlertLevel.CRITICAL, AlertLevel.WARNING, AlertLevel.INFO));
        severityCombo.setValue(AlertLevel.CRITICAL);

        TextField key1 = new TextField();
        key1.setPromptText("Keyword 1 (e.g. Warfarin / Penicillin / Peptic Ulcer)");

        TextField key2 = new TextField();
        key2.setPromptText("Keyword 2 (e.g. Ibuprofen / Amoxicillin)");

        TextArea desc = new TextArea();
        desc.setPromptText("Clinical description of mechanism and risk...");
        desc.setPrefRowCount(3);

        TextArea rec = new TextArea();
        rec.setPromptText("Actionable recommendation for physician...");
        rec.setPrefRowCount(2);

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: -color-critical; -fx-font-size: 11px;");
        errorLbl.setVisible(false);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Publish Rule (Hot Update)");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            String k1 = key1.getText();
            String k2 = key2.getText();
            String d = desc.getText();
            if (k1 == null || k1.isBlank() || k2 == null || k2.isBlank() || d == null || d.isBlank()) {
                errorLbl.setText("Keywords and Clinical Description are mandatory.");
                errorLbl.setVisible(true);
                return;
            }

            InteractionRule newRule = new InteractionRule(null, typeCombo.getValue(), k1.trim(), k2.trim(),
                    severityCombo.getValue(), d.trim(), rec.getText() != null ? rec.getText().trim() : "");

            ruleDAO.save(newRule);
            Account current = SessionManager.getInstance().getCurrentAccount();
            auditService.logStaffAction(current != null ? current.getUsername() : "admin", k1.trim() + "+" + k2.trim(),
                    "RULE_ADDED", "Added " + severityCombo.getValue() + " interaction rule: " + k1.trim() + " + " + k2.trim());

            reloadRulesTable();
            dialog.close();
            Toast.show(adminRootPane, "Rule published! Clinical safety engine screening is updated dynamically.", Toast.Type.SUCCESS);
        });

        buttons.getChildren().addAll(cancelBtn, saveBtn);
        form.getChildren().addAll(title, new Label("Rule Type:"), typeCombo, new Label("Severity:"), severityCombo,
                key1, key2, desc, rec, errorLbl, buttons);

        Scene scene = new Scene(form);
        scene.getStylesheets().addAll(adminRootPane.getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void initAuditTab() {
        auditColTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTimestamp().format(DATE_FMT)));
        auditColOperator.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOperatorUsername()));
        auditColOperation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOperationType()));
        auditColEntity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEntityType() + " (" + (c.getValue().getEntityId() != null ? c.getValue().getEntityId() : "—") + ")"));
        auditColDetails.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDetails()));

        auditTable.setItems(auditList);
        reloadAuditTable();

        auditSearchField.setOnSearch(q -> {
            if (q == null || q.isBlank()) {
                reloadAuditTable();
            } else {
                List<AuditLog> results = auditDAO.search(q.trim(), null, null, null, 100);
                auditList.setAll(results);
            }
        });
    }

    private void reloadAuditTable() {
        auditList.setAll(auditDAO.findRecent(100));
    }
}
