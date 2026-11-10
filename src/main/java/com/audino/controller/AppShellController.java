package com.audino.controller;

import com.audino.model.Account;
import com.audino.model.Role;
import com.audino.ui.components.Avatar;
import com.audino.ui.components.Chip;
import com.audino.ui.components.ConfirmDialog;
import com.audino.ui.navigation.CommandPalette;
import com.audino.ui.navigation.NavigationService;
import com.audino.ui.navigation.ThemeManager;
import com.audino.util.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main application shell controller hosting the global navigation rail, top status bar,
 * theme toggling, and central view router.
 */
public class AppShellController {

    private static final Logger logger = LoggerFactory.getLogger(AppShellController.class);
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("EEEE, dd-MMM-yyyy • hh:mm a");

    @FXML private VBox userProfileBox;
    @FXML private Label userNameLabel;
    @FXML private HBox userRoleContainer;
    @FXML private VBox navContainer;
    @FXML private Button navConsultationBtn;
    @FXML private Button navQueueBtn;
    @FXML private Button navAdminBtn;
    @FXML private Button navKioskBtn;
    @FXML private Button themeToggleBtn;
    @FXML private Button logoutBtn;

    @FXML private Label pageTitleLabel;
    @FXML private Label breadcrumbLabel;
    @FXML private Label liveClockLabel;
    @FXML private Button commandPaletteBtn;

    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        NavigationService.getInstance().setContentArea(contentArea);

        Account account = SessionManager.getInstance().getCurrentAccount();
        if (account == null) {
            logger.warn("No active session found in AppShellController. Returning to login.");
            NavigationService.getInstance().switchRootScene("/fxml/login.fxml", "Login");
            return;
        }

        // Configure User Profile Widget in Nav Rail
        userNameLabel.setText(account.getFullName());
        Avatar avatar = new Avatar(account.getFullName(), 18, "#14B8A6", "#0B1F2A");
        userProfileBox.getChildren().add(0, avatar);

        Chip.Variant roleVariant = switch (account.getRole()) {
            case ADMIN -> Chip.Variant.DANGER;
            case DOCTOR -> Chip.Variant.PRIMARY;
            case RECEPTIONIST -> Chip.Variant.INFO;
        };
        Chip roleChip = new Chip(account.getRole().name(), roleVariant);
        userRoleContainer.getChildren().setAll(roleChip);

        // Configure Role-Specific Navigation Visibility
        if (account.getRole() == Role.RECEPTIONIST) {
            navConsultationBtn.setVisible(false);
            navConsultationBtn.setManaged(false);
            navAdminBtn.setVisible(false);
            navAdminBtn.setManaged(false);
        } else if (account.getRole() == Role.DOCTOR) {
            navAdminBtn.setVisible(false);
            navAdminBtn.setManaged(false);
        }

        // Live Clock
        updateClock();
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(30), e -> updateClock()));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        // Theme Toggle Icon
        updateThemeToggleGraphic();

        // Nav Button Actions
        navConsultationBtn.setOnAction(e -> {
            setActiveNav(navConsultationBtn);
            NavigationService.getInstance().navigateTo("/fxml/doctor_consultation.fxml", "Consultation");
            updatePageHeader("Doctor Consultation", "Active Patient Prescribing & Clinical Decision Support");
        });

        navQueueBtn.setOnAction(e -> {
            setActiveNav(navQueueBtn);
            NavigationService.getInstance().navigateTo("/fxml/receptionist_dashboard.fxml", "Reception & Queue");
            updatePageHeader("Reception & Triage", "Patient Registration, Vitals Intake & Queue Token Issuance");
        });

        navAdminBtn.setOnAction(e -> {
            setActiveNav(navAdminBtn);
            NavigationService.getInstance().navigateTo("/fxml/admin_dashboard.fxml", "Admin Portal");
            updatePageHeader("Administration Portal", "Formulary, Clinical Interaction Rules & System Audit Logs");
        });

        navKioskBtn.setOnAction(e -> {
            NavigationService.getInstance().switchRootScene("/fxml/kiosk.fxml", "Patient Kiosk");
        });

        themeToggleBtn.setOnAction(e -> {
            ThemeManager.getInstance().toggleTheme(themeToggleBtn.getScene());
            updateThemeToggleGraphic();
        });

        logoutBtn.setOnAction(e -> {
            boolean confirm = ConfirmDialog.confirm(
                logoutBtn.getScene().getWindow(),
                "Sign Out",
                "Are you sure you want to log out of your session?",
                "Log Out",
                false
            );
            if (confirm) {
                NavigationService.getInstance().logout();
            }
        });

        commandPaletteBtn.setOnAction(e -> {
            CommandPalette.show(commandPaletteBtn.getScene().getWindow());
        });

        // Global Shortcut: Ctrl + K for Command Palette
        contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                KeyCombination ctrlK = new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN);
                newScene.getAccelerators().put(ctrlK, () -> CommandPalette.show(newScene.getWindow()));
            }
        });

        // Polymorphic Initial View Routing:
        // OOM: Polymorphism – routes to the dashboard defined by Account subclass without an 'if (role == ...)' chain.
        String initialFxml = account.getDashboardFxml();
        String initialTitle = account.getRole().name() + " Workspace";
        NavigationService.getInstance().navigateTo(initialFxml, initialTitle);
        updatePageHeader(account.getRole().name() + " Workspace", "Welcome back, " + account.getFullName());

        if (account.getRole() == Role.DOCTOR) {
            setActiveNav(navConsultationBtn);
        } else if (account.getRole() == Role.RECEPTIONIST) {
            setActiveNav(navQueueBtn);
        } else if (account.getRole() == Role.ADMIN) {
            setActiveNav(navAdminBtn);
        }
    }

    private void updateClock() {
        liveClockLabel.setText(LocalDateTime.now().format(CLOCK_FMT));
    }

    private void updateThemeToggleGraphic() {
        try {
            boolean dark = ThemeManager.getInstance().isDarkMode();
            FontIcon icon = new FontIcon(dark ? "mdi2w-white-balance-sunny" : "mdi2w-weather-night");
            icon.setIconSize(16);
            themeToggleBtn.setGraphic(icon);
            themeToggleBtn.setText(dark ? "Light Mode" : "Dark Mode");
        } catch (Exception ignored) {
        }
    }

    private void setActiveNav(Button activeBtn) {
        Button[] all = {navConsultationBtn, navQueueBtn, navAdminBtn};
        for (Button b : all) {
            if (b != null) {
                b.getStyleClass().remove("nav-btn-active");
            }
        }
        if (activeBtn != null && !activeBtn.getStyleClass().contains("nav-btn-active")) {
            activeBtn.getStyleClass().add("nav-btn-active");
        }
    }

    public void updatePageHeader(String title, String subtitle) {
        pageTitleLabel.setText(title);
        breadcrumbLabel.setText(subtitle != null ? subtitle : "");
    }
}
