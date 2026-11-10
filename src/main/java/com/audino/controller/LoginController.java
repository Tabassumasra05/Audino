package com.audino.controller;

import com.audino.dao.jdbc.AccountDAOJdbc;
import com.audino.dao.jdbc.AuditLogDAOJdbc;
import com.audino.exception.AccountDisabledException;
import com.audino.exception.AuthenticationException;
import com.audino.model.Account;
import com.audino.model.Role;
import com.audino.service.AuditService;
import com.audino.service.AuthService;
import com.audino.ui.navigation.NavigationService;
import com.audino.ui.navigation.ThemeManager;
import com.audino.util.ConfigurationManager;
import com.audino.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.awt.Toolkit;

/**
 * Controller for staff authentication, demo profile loading, and kiosk redirection.
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private VBox cardContainer;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button togglePasswordBtn;
    @FXML private Label capsLockWarning;
    @FXML private Label errorBanner;
    @FXML private Button loginBtn;
    @FXML private Button kioskBtn;

    private AuthService authService;
    private boolean passwordVisible = false;

    @FXML
    public void initialize() {
        DataSource ds = ConfigurationManager.getInstance().getDataSource();
        AuditService auditService = new AuditService(new AuditLogDAOJdbc(ds));
        this.authService = new AuthService(new AccountDAOJdbc(ds), auditService, 12);

        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
        capsLockWarning.setVisible(false);
        capsLockWarning.setManaged(false);

        // Synchronize password text between hidden and visible fields
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());

        // Caps Lock detection
        passwordField.setOnKeyPressed(e -> checkCapsLock());
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (passwordVisible) passwordTextField.requestFocus();
                else passwordField.requestFocus();
            }
        });

        passwordField.setOnAction(e -> handleLogin());
        passwordTextField.setOnAction(e -> handleLogin());
        loginBtn.setOnAction(e -> handleLogin());
        kioskBtn.setOnAction(e -> openKiosk());

        togglePasswordBtn.setOnAction(e -> togglePasswordVisibility());
    }

    private void checkCapsLock() {
        try {
            boolean isCapsLockOn = Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK);
            capsLockWarning.setVisible(isCapsLockOn);
            capsLockWarning.setManaged(isCapsLockOn);
        } catch (Exception ignored) {
        }
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordTextField.setVisible(passwordVisible);
        passwordTextField.setManaged(passwordVisible);
        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);

        try {
            FontIcon icon = new FontIcon(passwordVisible ? "mdi2e-eye-off" : "mdi2e-eye");
            icon.setIconSize(16);
            icon.setIconColor(javafx.scene.paint.Color.web("#64748B"));
            togglePasswordBtn.setGraphic(icon);
        } catch (Exception ignored) {
            togglePasswordBtn.setText(passwordVisible ? "Hide" : "Show");
        }

        if (passwordVisible) {
            passwordTextField.requestFocus();
            passwordTextField.positionCaret(passwordTextField.getText().length());
        } else {
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        errorBanner.setVisible(false);
        errorBanner.setManaged(false);

        try {
            Account account = authService.authenticate(username, password);
            logger.info("Login successful for user '{}' with role {}", account.getUsername(), account.getRole());

            // Open Shell
            NavigationService.getInstance().switchRootScene("/fxml/app_shell.fxml", "Audino EMR");
        } catch (AccountDisabledException e) {
            showError("Account Deactivated: " + e.getMessage());
            shakeCard();
        } catch (AuthenticationException e) {
            showError("Authentication Failed: " + e.getMessage());
            shakeCard();
        } catch (Exception e) {
            logger.error("Unexpected error during authentication: {}", e.getMessage(), e);
            showError("System error: " + e.getMessage());
            shakeCard();
        }
    }

    @FXML
    public void fillDoctorDemo() {
        usernameField.setText("doctor");
        passwordField.setText("1234567890");
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }

    @FXML
    public void fillAdminDemo() {
        usernameField.setText("admin");
        passwordField.setText("1234567890");
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }

    @FXML
    public void fillReceptionistDemo() {
        usernameField.setText("reception");
        passwordField.setText("1234567890");
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }

    @FXML
    private void openKiosk() {
        NavigationService.getInstance().switchRootScene("/fxml/kiosk.fxml", "Patient Self-Service Kiosk");
    }

    private void showError(String message) {
        errorBanner.setText(message);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);
    }

    private void shakeCard() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(0), new KeyValue(cardContainer.translateXProperty(), 0)),
            new KeyFrame(Duration.millis(50), new KeyValue(cardContainer.translateXProperty(), -10)),
            new KeyFrame(Duration.millis(100), new KeyValue(cardContainer.translateXProperty(), 10)),
            new KeyFrame(Duration.millis(150), new KeyValue(cardContainer.translateXProperty(), -8)),
            new KeyFrame(Duration.millis(200), new KeyValue(cardContainer.translateXProperty(), 8)),
            new KeyFrame(Duration.millis(250), new KeyValue(cardContainer.translateXProperty(), -4)),
            new KeyFrame(Duration.millis(300), new KeyValue(cardContainer.translateXProperty(), 4)),
            new KeyFrame(Duration.millis(350), new KeyValue(cardContainer.translateXProperty(), 0))
        );
        timeline.play();
    }
}
