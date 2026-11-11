package com.audino.ui.navigation;

import com.audino.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Singleton navigation and routing service managing scene views, shell content swaps,
 * animated transitions, and navigation history.
 */
public class NavigationService {

    private static final Logger logger = LoggerFactory.getLogger(NavigationService.class);
    private static volatile NavigationService instance;

    private Stage primaryStage;
    private StackPane contentArea;
    private String currentPageTitle = "Dashboard";
    private final Deque<String> historyStack = new ArrayDeque<>();
    private final Map<String, Object> routeControllers = new HashMap<>();

    private NavigationService() {
    }

    public static NavigationService getInstance() {
        if (instance == null) {
            synchronized (NavigationService.class) {
                if (instance == null) {
                    instance = new NavigationService();
                }
            }
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public StackPane getContentArea() {
        return contentArea;
    }

    public String getCurrentPageTitle() {
        return currentPageTitle;
    }

    /**
     * Replaces the root Scene of the primary stage (e.g. switching between Login, Shell, and Kiosk).
     */
    public void switchRootScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (primaryStage != null) {
                primaryStage.setTitle("Audino - " + title);

                if (primaryStage.getMinWidth() < 1000) {
                    primaryStage.setMinWidth(1000);
                }
                if (primaryStage.getMinHeight() < 600) {
                    primaryStage.setMinHeight(600);
                }

                Scene existingScene = primaryStage.getScene();
                if (existingScene != null) {
                    // Swap root in-place so Windows JavaFX does not reset the maximized stage bounds
                    existingScene.setRoot(root);
                    ThemeManager.getInstance().applyTheme(existingScene);
                } else {
                    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                    Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
                    ThemeManager.getInstance().applyTheme(scene);
                    primaryStage.setX(bounds.getMinX());
                    primaryStage.setY(bounds.getMinY());
                    primaryStage.setWidth(bounds.getWidth());
                    primaryStage.setHeight(bounds.getHeight());
                    primaryStage.setScene(scene);
                }

                primaryStage.setMaximized(true);
                primaryStage.show();
                Platform.runLater(() -> {
                    if (primaryStage != null) {
                        primaryStage.setMaximized(true);
                    }
                });
            }

            logger.info("Root scene switched to: {} ('{}')", fxmlPath, title);
        } catch (IOException e) {
            logger.error("Failed to load root scene FXML {}: {}", fxmlPath, e.getMessage(), e);
            throw new RuntimeException("View loading failure: " + fxmlPath, e);
        }
    }

    /**
     * Swaps the view within the central shell content area with a smooth cross-fade animation.
     */
    public <T> T navigateTo(String fxmlPath, String pageTitle, Consumer<T> controllerConsumer) {
        Objects.requireNonNull(contentArea, "Content area has not been initialized");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            T controller = loader.getController();

            if (controllerConsumer != null && controller != null) {
                controllerConsumer.accept(controller);
            }

            this.currentPageTitle = pageTitle;
            if (primaryStage != null) {
                primaryStage.setTitle("Audino - " + pageTitle);
            }

            // Cross-fade animation
            view.setOpacity(0.0);
            contentArea.getChildren().setAll(view);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), view);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            historyStack.push(fxmlPath);
            logger.info("Navigated content view to: {} ('{}')", fxmlPath, pageTitle);
            return controller;
        } catch (IOException e) {
            logger.error("Failed to navigate to content FXML {}: {}", fxmlPath, e.getMessage(), e);
            throw new RuntimeException("Content view loading failure: " + fxmlPath, e);
        }
    }

    public void navigateTo(String fxmlPath, String pageTitle) {
        navigateTo(fxmlPath, pageTitle, null);
    }

    public void logout() {
        SessionManager.getInstance().logout();
        switchRootScene("/fxml/login.fxml", "Login");
    }
}
