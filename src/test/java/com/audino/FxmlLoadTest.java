package com.audino;

import com.audino.controller.AppShellController;
import com.audino.controller.KioskController;
import com.audino.controller.LoginController;
import com.audino.model.Admin;
import com.audino.model.Doctor;
import com.audino.model.Receptionist;
import com.audino.ui.navigation.NavigationService;
import com.audino.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FXML Loading & Controller Initialization Verification")
public class FxmlLoadTest {

    private static Stage testStage;

    @BeforeAll
    static void initJavaFx() throws ExecutionException, InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }

        CompletableFuture<Stage> stageFuture = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stageFuture.complete(stage);
        });
        testStage = stageFuture.get();
        NavigationService.getInstance().setPrimaryStage(testStage);
    }

    @BeforeEach
    void setUp() {
        Doctor doc = new Doctor("doctor", "hash", "Dr. Arjun Mehta", "Cardiology", "MCI-48291");
        SessionManager.getInstance().setCurrentAccount(doc);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/fxml/login.fxml",
        "/fxml/app_shell.fxml",
        "/fxml/admin_dashboard.fxml",
        "/fxml/receptionist_dashboard.fxml",
        "/fxml/doctor_dashboard.fxml",
        "/fxml/doctor_consultation.fxml",
        "/fxml/kiosk.fxml"
    })
    @DisplayName("Verify FXML loads cleanly with controller")
    void testFxmlLoading(String fxmlPath) throws IOException {
        URL url = getClass().getResource(fxmlPath);
        assertThat(url).as("FXML file must exist on classpath: " + fxmlPath).isNotNull();

        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        Object controller = loader.getController();

        assertThat(root).as("Root Node must not be null for " + fxmlPath).isNotNull();
        assertThat(controller).as("Controller must not be null for " + fxmlPath).isNotNull();
    }

    @Test
    @DisplayName("Verify Login -> Dashboard Flow for Doctor")
    void testLoginToDoctorDashboardFlow() throws Exception {
        Doctor doctor = new Doctor("doctor", "hash", "Dr. Arjun Mehta", "Cardiology", "MCI-48291");
        SessionManager.getInstance().setCurrentAccount(doctor);

        FXMLLoader shellLoader = new FXMLLoader(getClass().getResource("/fxml/app_shell.fxml"));
        Parent shellRoot = shellLoader.load();
        AppShellController shellController = shellLoader.getController();

        assertThat(shellRoot).isNotNull();
        assertThat(shellController).isNotNull();
        assertThat(doctor.getDashboardFxml()).isEqualTo("/fxml/doctor_dashboard.fxml");

        StackPane contentArea = NavigationService.getInstance().getContentArea();
        assertThat(contentArea).isNotNull();
        assertThat(contentArea.getChildren()).isNotEmpty();
    }

    @Test
    @DisplayName("Verify Login -> Dashboard Flow for Admin")
    void testLoginToAdminDashboardFlow() throws Exception {
        Admin admin = new Admin("admin", "hash", "Dr. Sanjay Gupta");
        SessionManager.getInstance().setCurrentAccount(admin);

        FXMLLoader shellLoader = new FXMLLoader(getClass().getResource("/fxml/app_shell.fxml"));
        Parent shellRoot = shellLoader.load();
        AppShellController shellController = shellLoader.getController();

        assertThat(shellRoot).isNotNull();
        assertThat(shellController).isNotNull();
        assertThat(admin.getDashboardFxml()).isEqualTo("/fxml/admin_dashboard.fxml");

        StackPane contentArea = NavigationService.getInstance().getContentArea();
        assertThat(contentArea).isNotNull();
        assertThat(contentArea.getChildren()).isNotEmpty();
    }

    @Test
    @DisplayName("Verify Login -> Dashboard Flow for Receptionist")
    void testLoginToReceptionistDashboardFlow() throws Exception {
        Receptionist receptionist = new Receptionist("reception", "hash", "Neha Sharma");
        SessionManager.getInstance().setCurrentAccount(receptionist);

        FXMLLoader shellLoader = new FXMLLoader(getClass().getResource("/fxml/app_shell.fxml"));
        Parent shellRoot = shellLoader.load();
        AppShellController shellController = shellLoader.getController();

        assertThat(shellRoot).isNotNull();
        assertThat(shellController).isNotNull();
        assertThat(receptionist.getDashboardFxml()).isEqualTo("/fxml/receptionist_dashboard.fxml");

        StackPane contentArea = NavigationService.getInstance().getContentArea();
        assertThat(contentArea).isNotNull();
        assertThat(contentArea.getChildren()).isNotEmpty();
    }

    @Test
    @DisplayName("Verify Patient Kiosk button and Kiosk Navigation Flow")
    void testPatientKioskFlow() throws Exception {
        // Load Login Screen
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent loginRoot = loginLoader.load();
        LoginController loginController = loginLoader.getController();

        assertThat(loginRoot).isNotNull();
        assertThat(loginController).isNotNull();

        // Verify Demo profile fills
        loginController.fillDoctorDemo();
        Field userField = LoginController.class.getDeclaredField("usernameField");
        userField.setAccessible(true);
        TextField usernameTextField = (TextField) userField.get(loginController);
        assertThat(usernameTextField.getText()).isEqualTo("doctor");

        loginController.fillAdminDemo();
        assertThat(usernameTextField.getText()).isEqualTo("admin");

        loginController.fillReceptionistDemo();
        assertThat(usernameTextField.getText()).isEqualTo("reception");

        // Verify Kiosk Button exists and triggers Kiosk scene switch
        Field kioskBtnField = LoginController.class.getDeclaredField("kioskBtn");
        kioskBtnField.setAccessible(true);
        Button kioskBtn = (Button) kioskBtnField.get(loginController);
        assertThat(kioskBtn).isNotNull();
        assertThat(kioskBtn.getOnAction()).isNotNull();

        // Trigger kiosk transition
        Platform.runLater(kioskBtn::fire);

        // Load Kiosk directly to assert controller initialization
        FXMLLoader kioskLoader = new FXMLLoader(getClass().getResource("/fxml/kiosk.fxml"));
        Parent kioskRoot = kioskLoader.load();
        KioskController kioskController = kioskLoader.getController();

        assertThat(kioskRoot).isNotNull();
        assertThat(kioskController).isNotNull();

        Field returnBtnField = KioskController.class.getDeclaredField("returnToStaffBtn");
        returnBtnField.setAccessible(true);
        Button returnBtn = (Button) returnBtnField.get(kioskController);
        assertThat(returnBtn).isNotNull();
        assertThat(returnBtn.getOnAction()).isNotNull();
    }

    @Test
    @DisplayName("Verify Avatar computes initials strictly from first + last name, stripping titles and suffixes")
    void testAvatarInitialsComputation() {
        assertThat(com.audino.ui.components.Avatar.computeInitials("Dr. Sanjay Gupta (Admin)")).isEqualTo("SG");
        assertThat(com.audino.ui.components.Avatar.computeInitials("Sanjay Gupta")).isEqualTo("SG");
        assertThat(com.audino.ui.components.Avatar.computeInitials("Dr. Arjun Mehta")).isEqualTo("AM");
        assertThat(com.audino.ui.components.Avatar.computeInitials("Neha Sharma")).isEqualTo("NS");
        assertThat(com.audino.ui.components.Avatar.computeInitials("Admin")).isEqualTo("AD");
        assertThat(com.audino.ui.components.Avatar.computeInitials("")).isEqualTo("?");
        assertThat(com.audino.ui.components.Avatar.computeInitials(null)).isEqualTo("?");
    }
}
