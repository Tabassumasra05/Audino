package com.audino.ui.components;

import com.audino.model.AlertLevel;
import com.audino.model.PrescriptionAlert;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

/**
 * Clinical decision support alert card presenting drug-drug, drug-allergy, or drug-condition warnings.
 */
public class AlertCard extends VBox {

    private final PrescriptionAlert alert;
    private final Label titleLabel = new Label();
    private final Label descLabel = new Label();
    private final Label recLabel = new Label();
    private final Label overrideLabel = new Label();
    private Button overrideBtn;

    public AlertCard(PrescriptionAlert alert, Consumer<PrescriptionAlert> onOverrideRequested) {
        this.alert = alert;
        setSpacing(8);
        setPadding(new Insets(12, 16, 12, 16));

        String styleClass;
        String iconLiteral;
        if (alert.getLevel() == AlertLevel.CRITICAL) {
            styleClass = "alert-card alert-critical";
            iconLiteral = "mdi2a-alert-circle";
            setStyle("-fx-background-color: -color-critical-soft; -fx-border-color: -color-critical; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;");
        } else if (alert.getLevel() == AlertLevel.WARNING) {
            styleClass = "alert-card alert-warning";
            iconLiteral = "mdi2a-alert";
            setStyle("-fx-background-color: -color-warning-soft; -fx-border-color: -color-warning; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;");
        } else {
            styleClass = "alert-card alert-info";
            iconLiteral = "mdi2i-information";
            setStyle("-fx-background-color: -color-info-soft; -fx-border-color: -color-info; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;");
        }
        getStyleClass().addAll(styleClass.split(" "));

        // Header Row: Icon + Level Badge + Override Button
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        try {
            FontIcon icon = new FontIcon(iconLiteral);
            icon.setIconSize(18);
            if (alert.getLevel() == AlertLevel.CRITICAL) {
                icon.setStyle("-fx-icon-color: -color-critical;");
            } else if (alert.getLevel() == AlertLevel.WARNING) {
                icon.setStyle("-fx-icon-color: -color-warning;");
            } else {
                icon.setStyle("-fx-icon-color: -color-info;");
            }
            headerRow.getChildren().add(icon);
        } catch (Exception ignored) {
        }

        titleLabel.setText("[" + alert.getLevel() + "] " + alert.getType().name().replace('_', ' '));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        headerRow.getChildren().add(titleLabel);

        if (alert.getLevel() == AlertLevel.CRITICAL) {
            if (alert.isAcknowledged()) {
                Chip ackChip = new Chip("OVERRIDDEN", Chip.Variant.WARNING, "mdi2c-check-circle");
                headerRow.getChildren().add(ackChip);
            } else if (onOverrideRequested != null) {
                overrideBtn = new Button("Override Justification");
                overrideBtn.setStyle("-fx-background-color: -color-critical; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 6px; -fx-cursor: hand;");
                overrideBtn.setOnAction(e -> onOverrideRequested.accept(alert));
                headerRow.getChildren().add(overrideBtn);
            }
        }

        // Message / Description
        descLabel.setText(alert.getMessage());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text;");

        getChildren().addAll(headerRow, descLabel);

        // If overridden, show note
        if (alert.isAcknowledged() && alert.getOverrideNote() != null && !alert.getOverrideNote().isBlank()) {
            overrideLabel.setText("Override Note: \"" + alert.getOverrideNote() + "\" (by " + alert.getOverriddenBy() + ")");
            overrideLabel.setWrapText(true);
            overrideLabel.setStyle("-fx-font-style: italic; -fx-font-size: 11px; -fx-text-fill: -color-text-secondary;");
            getChildren().add(overrideLabel);
        }
    }

    public PrescriptionAlert getAlert() {
        return alert;
    }
}
