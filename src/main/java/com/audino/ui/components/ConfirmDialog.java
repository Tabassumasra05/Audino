package com.audino.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

/**
 * Clean styled confirmation and justification modal dialog.
 */
public class ConfirmDialog {

    public static boolean confirm(Window owner, String title, String message, String confirmText, boolean isDestructive) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle(title);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: -color-surface; -fx-min-width: 420px;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        try {
            FontIcon icon = new FontIcon(isDestructive ? "mdi2a-alert-circle" : "mdi2h-help-circle");
            icon.setIconSize(28);
            icon.setStyle("-fx-icon-color: " + (isDestructive ? "-color-critical" : "-color-primary") + ";");
            header.getChildren().add(icon);
        } catch (Exception ignored) {
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text;");
        header.getChildren().add(titleLabel);

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-secondary;");

        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");

        Button confirmBtn = new Button(confirmText != null ? confirmText : "Confirm");
        if (isDestructive) {
            confirmBtn.setStyle("-fx-background-color: -color-critical; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        } else {
            confirmBtn.getStyleClass().add("btn-primary");
        }

        final boolean[] result = {false};

        cancelBtn.setOnAction(e -> {
            result[0] = false;
            dialog.close();
        });

        confirmBtn.setOnAction(e -> {
            result[0] = true;
            dialog.close();
        });

        buttons.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().addAll(header, msgLabel, buttons);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        dialog.setScene(scene);
        dialog.showAndWait();

        return result[0];
    }

    /**
     * Prompts for mandatory clinical override justification with a minimum length requirement.
     */
    public static Optional<String> promptClinicalOverride(Window owner, String alertMessage) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Mandatory Clinical Override Justification");

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: -color-surface; -fx-min-width: 500px;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        try {
            FontIcon icon = new FontIcon("mdi2a-shield-alert");
            icon.setIconSize(24);
            icon.setStyle("-fx-icon-color: -color-critical;");
            header.getChildren().add(icon);
        } catch (Exception ignored) {
        }
        Label title = new Label("Critical Safety Override Justification");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-critical;");
        header.getChildren().add(title);

        Label alertText = new Label(alertMessage);
        alertText.setWrapText(true);
        alertText.setStyle("-fx-font-size: 12px; -fx-padding: 10px; -fx-background-color: -color-critical-soft; -fx-border-color: -color-critical; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        Label prompt = new Label("Please enter a mandatory clinical rationale (minimum 15 characters). This event will be recorded in the forensic audit trail:");
        prompt.setWrapText(true);
        prompt.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text;");

        TextArea noteArea = new TextArea();
        noteArea.setPromptText("e.g. Daily INR monitored; patient under continuous inpatient observation...");
        noteArea.setPrefRowCount(3);
        noteArea.setWrapText(true);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: -color-critical; -fx-font-size: 11px;");
        errorLabel.setVisible(false);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel Prescription");
        cancelBtn.getStyleClass().add("btn-secondary");

        Button submitBtn = new Button("Submit Override");
        submitBtn.setStyle("-fx-background-color: -color-critical; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 16px; -fx-cursor: hand;");

        final String[] result = {null};

        cancelBtn.setOnAction(e -> dialog.close());

        submitBtn.setOnAction(e -> {
            String text = noteArea.getText();
            if (text == null || text.trim().length() < 15) {
                errorLabel.setText("Justification rationale must be at least 15 characters long.");
                errorLabel.setVisible(true);
                return;
            }
            result[0] = text.trim();
            dialog.close();
        });

        buttons.getChildren().addAll(cancelBtn, submitBtn);
        root.getChildren().addAll(header, alertText, prompt, noteArea, errorLabel, buttons);

        Scene scene = new Scene(root);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.setScene(scene);
        dialog.showAndWait();

        return Optional.ofNullable(result[0]);
    }
}
