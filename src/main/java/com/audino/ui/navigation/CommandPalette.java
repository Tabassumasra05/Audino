package com.audino.ui.navigation;

import com.audino.dao.MedicationDAO;
import com.audino.dao.PatientDAO;
import com.audino.dao.jdbc.MedicationDAOJdbc;
import com.audino.dao.jdbc.PatientDAOJdbc;
import com.audino.model.Medication;
import com.audino.model.Patient;
import com.audino.ui.components.SearchField;
import com.audino.util.ConfigurationManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * Command Palette (Ctrl + K) for rapid keyboard navigation, clinical search, and quick actions.
 */
public class CommandPalette {

    public record PaletteItem(String title, String category, String iconLiteral, Runnable action) {}

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: -color-surface; -fx-border-color: -color-border; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 8); -fx-min-width: 550px;");

        // Search Input
        SearchField searchField = new SearchField("Type a command, patient name, or medication...");
        searchField.setStyle("-fx-background-color: -color-surface-alt; -fx-border-color: -color-primary; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6px 12px; -fx-font-size: 14px;");

        // Action / Result List
        ListView<PaletteItem> listView = new ListView<>();
        listView.setPrefHeight(280);
        listView.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        List<PaletteItem> defaultCommands = buildDefaultCommands(stage, owner);
        ObservableList<PaletteItem> items = FXCollections.observableArrayList(defaultCommands);
        listView.setItems(items);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PaletteItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER_LEFT);
                    box.setPadding(new Insets(6, 8, 6, 8));

                    try {
                        FontIcon icon = new FontIcon(item.iconLiteral());
                        icon.setIconSize(18);
                        icon.setStyle("-fx-icon-color: -color-primary;");
                        box.getChildren().add(icon);
                    } catch (Exception ignored) {
                    }

                    VBox textContainer = new VBox(2);
                    Label title = new Label(item.title());
                    title.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: -color-text;");

                    Label cat = new Label(item.category().toUpperCase());
                    cat.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");

                    textContainer.getChildren().addAll(title, cat);
                    HBox.setHgrow(textContainer, Priority.ALWAYS);
                    box.getChildren().add(textContainer);

                    setGraphic(box);
                }
            }
        });

        // Search filtering
        searchField.setOnSearch(query -> {
            if (query == null || query.isBlank()) {
                items.setAll(defaultCommands);
            } else {
                List<PaletteItem> filtered = new ArrayList<>();
                String q = query.trim().toLowerCase();

                // 1. Search default commands
                for (PaletteItem cmd : defaultCommands) {
                    if (cmd.title().toLowerCase().contains(q) || cmd.category().toLowerCase().contains(q)) {
                        filtered.add(cmd);
                    }
                }

                // 2. Query Patients from DB
                try {
                    PatientDAO patientDAO = new PatientDAOJdbc(ConfigurationManager.getInstance().getDataSource());
                    List<Patient> patients = patientDAO.search(q);
                    for (int i = 0; i < Math.min(4, patients.size()); i++) {
                        Patient p = patients.get(i);
                        filtered.add(new PaletteItem(
                            p.getFullName() + " (" + p.getPatientId() + ", " + p.getPhone() + ")",
                            "Patient Record",
                            "mdi2a-account",
                            () -> {
                                NavigationService.getInstance().navigateTo("/fxml/doctor_consultation.fxml", "Consultation");
                            }
                        ));
                    }
                } catch (Exception ignored) {
                }

                // 3. Query Formulary Medications from DB
                try {
                    MedicationDAO medDAO = new MedicationDAOJdbc(ConfigurationManager.getInstance().getDataSource());
                    List<Medication> meds = medDAO.search(q);
                    for (int i = 0; i < Math.min(4, meds.size()); i++) {
                        Medication m = meds.get(i);
                        filtered.add(new PaletteItem(
                            m.getBrandName() + " (" + m.getGenericName() + " " + m.getStrength() + ")",
                            "Formulary Drug",
                            "mdi2p-pill",
                            () -> {
                                NavigationService.getInstance().navigateTo("/fxml/doctor_consultation.fxml", "Consultation");
                            }
                        ));
                    }
                } catch (Exception ignored) {
                }

                items.setAll(filtered);
                if (!items.isEmpty()) {
                    listView.getSelectionModel().select(0);
                }
            }
        });

        // Key bindings: Enter to select, Escape to close
        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            } else if (e.getCode() == KeyCode.ENTER) {
                PaletteItem selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.action() != null) {
                    stage.close();
                    selected.action().run();
                }
            }
        });

        listView.setOnMouseClicked(e -> {
            PaletteItem selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null && selected.action() != null) {
                stage.close();
                selected.action().run();
            }
        });

        Label tip = new Label("Press ↑ / ↓ to navigate, ↵ Enter to select, Esc to dismiss");
        tip.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        root.getChildren().addAll(searchField, listView, tip);

        Scene scene = new Scene(root);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        stage.setScene(scene);
        stage.show();
        searchField.requestSearchFocus();
    }

    private static List<PaletteItem> buildDefaultCommands(Stage dialog, Window owner) {
        List<PaletteItem> list = new ArrayList<>();

        list.add(new PaletteItem("Doctor Consultation Portal", "Navigation", "mdi2s-stethoscope", () -> {
            NavigationService.getInstance().navigateTo("/fxml/doctor_consultation.fxml", "Doctor Consultation");
        }));

        list.add(new PaletteItem("Receptionist Triage & Queue", "Navigation", "mdi2c-clipboard-account", () -> {
            NavigationService.getInstance().navigateTo("/fxml/receptionist_dashboard.fxml", "Receptionist Portal");
        }));

        list.add(new PaletteItem("Admin Dashboard & Analytics", "Navigation", "mdi2s-shield-account", () -> {
            NavigationService.getInstance().navigateTo("/fxml/admin_dashboard.fxml", "Admin Dashboard");
        }));

        list.add(new PaletteItem("Open Patient Self-Service Kiosk", "Kiosk", "mdi2k-kiosk", () -> {
            NavigationService.getInstance().switchRootScene("/fxml/kiosk.fxml", "Patient Kiosk");
        }));

        list.add(new PaletteItem("Toggle Dark / Light Mode", "Theme", "mdi2t-theme-light-dark", () -> {
            if (owner != null && owner.getScene() != null) {
                ThemeManager.getInstance().toggleTheme(owner.getScene());
            }
        }));

        list.add(new PaletteItem("Sign Out / Logout", "Session", "mdi2l-logout", () -> {
            NavigationService.getInstance().logout();
        }));

        return list;
    }
}
