package com.audino;

import com.audino.controller.AdminDashboardController;
import com.audino.controller.AppShellController;
import com.audino.controller.DoctorConsultationController;
import com.audino.controller.KioskController;
import com.audino.controller.LoginController;
import com.audino.controller.ReceptionistController;
import com.audino.model.Admin;
import com.audino.model.Doctor;
import com.audino.model.Medication;
import com.audino.model.Receptionist;
import com.audino.model.Token;
import com.audino.ui.navigation.NavigationService;
import com.audino.ui.navigation.ThemeManager;
import com.audino.ui.navigation.ThemeManager.Theme;
import com.audino.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * High-fidelity visual test and screenshot generator.
 * Renders all pages across Admin, Doctor, Receptionist, Kiosk, and Login flows
 * at the university-specified 1366x768 resolution in both Light and Dark themes.
 */
@DisplayName("Comprehensive UI Screenshot Generation (1366x768 Light & Dark)")
public class ScreenshotGenerator {

    private static Stage testStage;
    private static final int SCREEN_WIDTH = 1366;
    private static final int SCREEN_HEIGHT = 768;
    private static final File OUTPUT_DIR = new File("docs/screenshots");

    @BeforeAll
    static void initJavaFx() throws ExecutionException, InterruptedException, IOException {
        Files.createDirectories(OUTPUT_DIR.toPath());

        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }

        CompletableFuture<Stage> stageFuture = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle("Audino: Intelligent Clinical Decision Support & EMR System");
            stage.setWidth(SCREEN_WIDTH);
            stage.setHeight(SCREEN_HEIGHT);
            stage.show();
            stageFuture.complete(stage);
        });
        testStage = stageFuture.get();
        NavigationService.getInstance().setPrimaryStage(testStage);
    }

    private static void saveSnapshot(Scene scene, String filename) throws Exception {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                scene.getRoot().applyCss();
                scene.getRoot().layout();

                WritableImage fxImg = scene.snapshot(null);
                BufferedImage bImg = toBufferedImage(fxImg);

                // Composite any open PopupWindows (e.g., ComboBox dropdown popups)
                Window mainWin = scene.getWindow();
                if (mainWin != null) {
                    java.awt.Graphics2D g2d = bImg.createGraphics();
                    SnapshotParameters popParams = new SnapshotParameters();
                    popParams.setFill(Color.TRANSPARENT);
                    for (Window win : Window.getWindows()) {
                        if (win instanceof PopupWindow && win.isShowing() && win.getScene() != null && win.getScene().getRoot() != null) {
                            win.getScene().getRoot().applyCss();
                            win.getScene().getRoot().layout();
                            WritableImage popFx = win.getScene().getRoot().snapshot(popParams, null);
                            BufferedImage popBImg = toBufferedImage(popFx);
                            int offsetX = (int) Math.round(win.getX() - (mainWin.getX() + scene.getX()));
                            int offsetY = (int) Math.round(win.getY() - (mainWin.getY() + scene.getY()));
                            g2d.drawImage(popBImg, offsetX, offsetY, null);
                        }
                    }
                    g2d.dispose();
                }

                File file = new File(OUTPUT_DIR, filename);
                ImageIO.write(bImg, "png", file);
                future.complete(null);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });
        future.get();
    }

    private static BufferedImage toBufferedImage(WritableImage fxImg) {
        int w = (int) fxImg.getWidth();
        int h = (int) fxImg.getHeight();
        BufferedImage bImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = fxImg.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bImg.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return bImg;
    }

    private static void runFx(Runnable action) throws Exception {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        future.get();
    }

    @Test
    @Timeout(90)
    @DisplayName("Generate all role screenshots across Light and Dark themes")
    void generateAllScreenshots() throws Exception {
        for (Theme theme : new Theme[]{Theme.LIGHT, Theme.DARK}) {
            String suffix = (theme == Theme.LIGHT) ? "_light.png" : "_dark.png";

            // 1. LOGIN SCREEN
            runFx(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);
                    ThemeManager.getInstance().setTheme(scene, theme);
                    testStage.setScene(scene);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Thread.sleep(150);
            saveSnapshot(testStage.getScene(), "login" + suffix);

            // 2. PATIENT KIOSK SCREEN
            runFx(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/kiosk.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);
                    ThemeManager.getInstance().setTheme(scene, theme);
                    testStage.setScene(scene);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Thread.sleep(150);
            saveSnapshot(testStage.getScene(), "kiosk" + suffix);

            // 3. ADMIN WORKSPACE (app_shell hosting admin_dashboard)
            final Admin admin = new Admin("admin", "hash", "Sanjay Gupta");
            final TabPane[] adminTabPaneHolder = new TabPane[1];
            runFx(() -> {
                try {
                    SessionManager.getInstance().setCurrentAccount(admin);
                    FXMLLoader shellLoader = new FXMLLoader(getClass().getResource("/fxml/app_shell.fxml"));
                    Parent shellRoot = shellLoader.load();
                    Scene scene = new Scene(shellRoot, SCREEN_WIDTH, SCREEN_HEIGHT);
                    ThemeManager.getInstance().setTheme(scene, theme);
                    testStage.setScene(scene);

                    adminTabPaneHolder[0] = (TabPane) shellRoot.lookup(".clinical-tab-pane");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Thread.sleep(200);

            // Admin Tab 0: Overview & Analytics + Recent Audit Log
            runFx(() -> adminTabPaneHolder[0].getSelectionModel().select(0));
            Thread.sleep(150);
            saveSnapshot(testStage.getScene(), "admin_overview" + suffix);

            // Admin Tab 1: Staff Accounts
            runFx(() -> adminTabPaneHolder[0].getSelectionModel().select(1));
            Thread.sleep(150);
            saveSnapshot(testStage.getScene(), "admin_staff" + suffix);

            // Admin Tab 2: Formulary
            runFx(() -> adminTabPaneHolder[0].getSelectionModel().select(2));
            Thread.sleep(150);
            saveSnapshot(testStage.getScene(), "admin_formulary" + suffix);

            // Admin Tab 3: Safety Rules
            runFx(() -> adminTabPaneHolder[0].getSelectionModel().select(3));
            Thread.sleep(150);
            saveSnapshot(testStage.getScene(), "admin_rules" + suffix);

            // Admin Tab 4: Audit Logs
            runFx(() -> adminTabPaneHolder[0].getSelectionModel().select(4));
            Thread.sleep(150);
            saveSnapshot(testStage.getScene(), "admin_audit" + suffix);

            // 4. RECEPTIONIST WORKSPACE
            final Receptionist receptionist = new Receptionist("reception", "hash", "Neha Sharma");
            runFx(() -> {
                try {
                    SessionManager.getInstance().setCurrentAccount(receptionist);
                    FXMLLoader shellLoader = new FXMLLoader(getClass().getResource("/fxml/app_shell.fxml"));
                    Parent shellRoot = shellLoader.load();
                    Scene scene = new Scene(shellRoot, SCREEN_WIDTH, SCREEN_HEIGHT);
                    ThemeManager.getInstance().setTheme(scene, theme);
                    testStage.setScene(scene);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Thread.sleep(350);
            saveSnapshot(testStage.getScene(), "reception_register" + suffix);

            // Receptionist Queue View: test Doctor ComboBox popup & search interaction
            final javafx.scene.control.ComboBox<?>[] docComboHolder = new javafx.scene.control.ComboBox[1];
            runFx(() -> {
                javafx.scene.Node combo = testStage.getScene().getRoot().lookup("#doctorAssignCombo");
                if (combo instanceof javafx.scene.control.ComboBox<?> cb) {
                    docComboHolder[0] = cb;
                    cb.show();
                }
            });
            Thread.sleep(200);
            saveSnapshot(testStage.getScene(), "reception_queue" + suffix);
            runFx(() -> {
                if (docComboHolder[0] != null) {
                    docComboHolder[0].hide();
                }
            });

            // 5. DOCTOR HOME DASHBOARD
            final Doctor doctor = new Doctor("doctor", "hash", "Dr. Arjun Mehta", "Cardiology", "MCI-48291");
            runFx(() -> {
                try {
                    SessionManager.getInstance().setCurrentAccount(doctor);
                    FXMLLoader shellLoader = new FXMLLoader(getClass().getResource("/fxml/app_shell.fxml"));
                    Parent shellRoot = shellLoader.load();
                    Scene scene = new Scene(shellRoot, SCREEN_WIDTH, SCREEN_HEIGHT);
                    ThemeManager.getInstance().setTheme(scene, theme);
                    testStage.setScene(scene);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Thread.sleep(300);
            saveSnapshot(testStage.getScene(), "doctor_dashboard" + suffix);

            // 6. DOCTOR CONSULTATION (Prescribing & Safety Radar)
            final TabPane[] docTabPaneHolder = new TabPane[1];
            final DoctorConsultationController[] docControllerHolder = new DoctorConsultationController[1];
            final StackPane[] docRootHolder = new StackPane[1];
            runFx(() -> {
                try {
                    FXMLLoader docLoader = new FXMLLoader(getClass().getResource("/fxml/doctor_consultation.fxml"));
                    Parent docRoot = docLoader.load();
                    docRootHolder[0] = (StackPane) docRoot;
                    docControllerHolder[0] = docLoader.getController();

                    StackPane contentArea = NavigationService.getInstance().getContentArea();
                    contentArea.getChildren().setAll(docRoot);

                    docTabPaneHolder[0] = (TabPane) docRoot.lookup(".clinical-tab-pane");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Thread.sleep(300);

            // Doctor Tab 0: Overview & Vitals Trends
            runFx(() -> docTabPaneHolder[0].getSelectionModel().select(0));
            Thread.sleep(200);
            saveSnapshot(testStage.getScene(), "doctor_overview" + suffix);

            // Doctor Tab 1: Prescribe & Safety Radar
            // Add Amoxicillin (Penicillin cross-allergy) to trigger CDS Safety Radar Contraindication
            runFx(() -> {
                docTabPaneHolder[0].getSelectionModel().select(1);
                try {
                    Field formTableField = DoctorConsultationController.class.getDeclaredField("formularyTable");
                    formTableField.setAccessible(true);
                    TableView<Medication> formTable = (TableView<Medication>) formTableField.get(docControllerHolder[0]);
                    
                    // Find Amoxicillin (or first item if not found)
                    int selIndex = 0;
                    for (int i = 0; i < formTable.getItems().size(); i++) {
                        if ("Amoxicillin".equalsIgnoreCase(formTable.getItems().get(i).getGenericName())) {
                            selIndex = i;
                            break;
                        }
                    }
                    formTable.getSelectionModel().select(selIndex);

                    Field addBtnField = DoctorConsultationController.class.getDeclaredField("addItemBtn");
                    addBtnField.setAccessible(true);
                    Button addBtn = (Button) addBtnField.get(docControllerHolder[0]);
                    addBtn.fire();

                    // Remove transient toast so prescribing radar is unobstructed
                    docRootHolder[0].getChildren().removeIf(node -> node instanceof com.audino.ui.components.Toast);
                } catch (Exception ignored) {
                }
            });
            Thread.sleep(300);
            saveSnapshot(testStage.getScene(), "doctor_prescribe" + suffix);

            // Doctor Tab 2: Consultation History
            runFx(() -> {
                docTabPaneHolder[0].getSelectionModel().select(2);
                docRootHolder[0].getChildren().removeIf(node -> node instanceof com.audino.ui.components.Toast);
            });
            Thread.sleep(200);
            saveSnapshot(testStage.getScene(), "doctor_history" + suffix);
        }
    }

    @AfterAll
    static void tearDown() {
        Platform.runLater(() -> {
            try {
                if (testStage != null) {
                    testStage.close();
                }
                Platform.exit();
            } catch (Exception ignored) {
            }
        });
    }
}
