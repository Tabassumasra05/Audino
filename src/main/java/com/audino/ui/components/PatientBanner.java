package com.audino.ui.components;

import com.audino.model.Patient;
import com.audino.model.PatientAllergy;
import com.audino.model.PatientCondition;
import com.audino.model.PatientVitals;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Sticky patient safety banner showing identity, prominent red allergy alerts,
 * amber chronic conditions, and current vital indicators.
 */
public class PatientBanner extends HBox {

    private final Avatar avatar;
    private final Label nameLabel = new Label();
    private final Label metaLabel = new Label();
    private final FlowPane allergyPane = new FlowPane(6, 6);
    private final FlowPane conditionPane = new FlowPane(6, 6);
    private final HBox vitalsContainer = new HBox(16);

    public PatientBanner() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        setPadding(new Insets(12, 20, 12, 20));
        getStyleClass().addAll("surface-card", "patient-safety-banner");
        setStyle("-fx-background-color: -color-surface; -fx-border-color: -color-border; -fx-border-width: 0 0 1px 0;");

        // 1. Patient Avatar
        avatar = new Avatar("?", 22);
        getChildren().add(avatar);

        // 2. Patient Demographics & ID
        VBox identityBox = new VBox(2);
        identityBox.setAlignment(Pos.CENTER_LEFT);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: -color-text;");
        metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-secondary;");
        identityBox.getChildren().addAll(nameLabel, metaLabel);
        getChildren().add(identityBox);

        // Separator
        Separator sep1 = new Separator();
        sep1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        getChildren().add(sep1);

        // 3. Clinical Alerts Section (Allergies + Conditions)
        VBox alertsBox = new VBox(4);
        alertsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(alertsBox, Priority.ALWAYS);

        HBox allergyRow = new HBox(6);
        allergyRow.setAlignment(Pos.CENTER_LEFT);
        Label allgTitle = new Label("ALLERGIES:");
        allgTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-critical;");
        allergyRow.getChildren().addAll(allgTitle, allergyPane);

        HBox condRow = new HBox(6);
        condRow.setAlignment(Pos.CENTER_LEFT);
        Label condTitle = new Label("CONDITIONS:");
        condTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-warning;");
        condRow.getChildren().addAll(condTitle, conditionPane);

        alertsBox.getChildren().addAll(allergyRow, condRow);
        getChildren().add(alertsBox);

        // Separator
        Separator sep2 = new Separator();
        sep2.setOrientation(javafx.geometry.Orientation.VERTICAL);
        getChildren().add(sep2);

        // 4. Vitals Summary Indicators
        vitalsContainer.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(vitalsContainer);
    }

    public void setPatient(Patient patient, PatientVitals latestVitals) {
        if (patient == null) {
            setVisible(false);
            setManaged(false);
            return;
        }

        setVisible(true);
        setManaged(true);

        nameLabel.setText(patient.getFullName());
        metaLabel.setText(patient.getPatientId() + " • " + patient.getAge() + " yrs • " + patient.getGender() + " • " + patient.getPhone());

        // Recreate avatar with patient initials
        getChildren().set(0, new Avatar(patient.getFullName(), 22));

        // Allergies
        allergyPane.getChildren().clear();
        if (patient.getAllergies().isEmpty()) {
            Label noAllg = new Label("No known drug allergies (NKDA)");
            noAllg.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-style: italic;");
            allergyPane.getChildren().add(noAllg);
        } else {
            for (PatientAllergy allergy : patient.getAllergies()) {
                Chip chip = new Chip(allergy.getAllergen(), Chip.Variant.DANGER, "mdi2a-alert-circle");
                allergyPane.getChildren().add(chip);
            }
        }

        // Conditions
        conditionPane.getChildren().clear();
        if (patient.getConditions().isEmpty()) {
            Label noCond = new Label("None recorded");
            noCond.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-style: italic;");
            conditionPane.getChildren().add(noCond);
        } else {
            for (PatientCondition cond : patient.getConditions()) {
                Chip chip = new Chip(cond.getConditionName(), Chip.Variant.WARNING, "mdi2h-heart-pulse");
                conditionPane.getChildren().add(chip);
            }
        }

        // Vitals
        vitalsContainer.getChildren().clear();
        if (latestVitals != null) {
            vitalsContainer.getChildren().addAll(
                createVitalItem("BP", latestVitals.getBloodPressureString(), "mmHg", latestVitals.getSystolicBp() > 140 ? "#DC2626" : "#0F172A"),
                createVitalItem("PULSE", String.valueOf(latestVitals.getPulseBpm()), "bpm", "#0F172A"),
                createVitalItem("SpO2", latestVitals.getSpo2Percent() + "%", "", latestVitals.getSpo2Percent() < 95 ? "#DC2626" : "#0F172A"),
                createVitalItem("BMI", String.format("%.1f", latestVitals.calculateBmi()), latestVitals.getBmiCategory(), "#0F766E")
            );
        } else {
            Label noVit = new Label("No vitals recorded today");
            noVit.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-style: italic;");
            vitalsContainer.getChildren().add(noVit);
        }
    }

    private VBox createVitalItem(String label, String value, String unit, String color) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");

        Label val = new Label(value + (unit.isEmpty() ? "" : " " + unit));
        val.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        box.getChildren().addAll(lbl, val);
        return box;
    }
}
