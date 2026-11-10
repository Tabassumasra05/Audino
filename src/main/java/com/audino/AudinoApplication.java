package com.audino;

import com.audino.util.ConfigurationManager;
import com.audino.util.DataSeeder;
import com.audino.util.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.net.URL;

/**
 * Main JavaFX application entry point for Audino EMR.
 * Orchestrates configuration loading, database bootstrapping, font registration,
 * and view presentation.
 */
public class AudinoApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(AudinoApplication.class);

    private boolean isDarkTheme = false;
    private Scene mainScene;

    @Override
    public void init() throws Exception {
        logger.info("Initializing Audino Application runtime...");

        // 1. Initialize configuration & database connection pool (with embedded fallback)
        ConfigurationManager config = ConfigurationManager.getInstance();
        DataSource dataSource = config.getDataSource();

        // 2. Initialize database schema
        DatabaseInitializer dbInit = new DatabaseInitializer(dataSource);
        dbInit.initialize();

        // 3. Seed clinical demo dataset if database is empty
        DataSeeder seeder = new DataSeeder(dataSource);
        seeder.seedIfEmpty();

        // 4. Load bundled Inter font files dynamically
        loadBundledFonts();
    }

    private void loadBundledFonts() {
        String[] fontFiles = {
            "fonts/Inter-Regular.ttf",
            "fonts/Inter.ttf"
        };

        for (String file : fontFiles) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(file)) {
                if (in != null) {
                    Font font = Font.loadFont(in, 13.0);
                    if (font != null) {
                        logger.info("Registered typography font: {} ({})", font.getName(), font.getFamily());
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not load font file {}: {}", file, e.getMessage());
            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting JavaFX Primary Stage...");

        URL fxmlUrl = getClass().getClassLoader().getResource("fxml/startup.fxml");
        if (fxmlUrl == null) {
            throw new IllegalStateException("Cannot find fxml/startup.fxml on classpath");
        }

        Parent root = FXMLLoader.load(fxmlUrl);
        mainScene = new Scene(root, 1150, 700);

        // Apply custom clinical CSS design system
        applyTheme("light.css");

        primaryStage.setTitle("Audino: Intelligent Clinical Decision Support & EMR System");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setMaximized(true);
        primaryStage.setScene(mainScene);

        // Wire interactive demo handlers on startup view
        Button themeToggleBtn = (Button) mainScene.lookup("#themeToggleBtn");
        if (themeToggleBtn != null) {
            themeToggleBtn.setOnAction(e -> toggleTheme());
        }

        Label dbEngineLabel = (Label) mainScene.lookup("#dbEngineLabel");
        if (dbEngineLabel != null) {
            boolean embedded = ConfigurationManager.getInstance().isEmbedded();
            dbEngineLabel.setText(embedded ? "Embedded PostgreSQL (Zero-Setup)" : "External PostgreSQL (localhost:5432)");
        }

        primaryStage.show();
        logger.info("Audino Stage displayed successfully.");
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        applyTheme(isDarkTheme ? "dark.css" : "light.css");
    }

    private void applyTheme(String themeFile) {
        if (mainScene == null) return;
        mainScene.getStylesheets().clear();

        URL themeUrl = getClass().getClassLoader().getResource("css/" + themeFile);
        URL appCssUrl = getClass().getClassLoader().getResource("css/application.css");

        if (themeUrl != null) {
            mainScene.getStylesheets().add(themeUrl.toExternalForm());
        }
        if (appCssUrl != null) {
            mainScene.getStylesheets().add(appCssUrl.toExternalForm());
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Audino application shutdown requested.");
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
