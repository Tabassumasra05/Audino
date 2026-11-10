package com.audino.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * Step progress indicator component for multi-step patient intake workflows.
 */
public class Stepper extends HBox {

    public record StepInfo(String title, String description) {}

    private final List<StepInfo> steps;
    private int currentStep = 0;
    private final List<StepNode> stepNodes = new ArrayList<>();

    public Stepper(List<StepInfo> steps) {
        this.steps = steps;
        setAlignment(Pos.CENTER);
        setSpacing(12);
        getStyleClass().add("stepper-container");

        for (int i = 0; i < steps.size(); i++) {
            StepNode node = new StepNode(i + 1, steps.get(i));
            stepNodes.add(node);
            getChildren().add(node);

            // Add connector line between steps
            if (i < steps.size() - 1) {
                Line connector = new Line(0, 0, 50, 0);
                connector.setStyle("-fx-stroke: -color-border; -fx-stroke-width: 2;");
                HBox.setHgrow(connector, Priority.ALWAYS);
                getChildren().add(connector);
            }
        }

        updateStates();
    }

    public void setCurrentStep(int stepIndex) {
        this.currentStep = Math.max(0, Math.min(stepIndex, steps.size() - 1));
        updateStates();
    }

    public int getCurrentStep() {
        return currentStep;
    }

    private void updateStates() {
        for (int i = 0; i < stepNodes.size(); i++) {
            StepNode node = stepNodes.get(i);
            if (i < currentStep) {
                node.setState(StepState.COMPLETED);
            } else if (i == currentStep) {
                node.setState(StepState.ACTIVE);
            } else {
                node.setState(StepState.UPCOMING);
            }
        }
    }

    public enum StepState {
        ACTIVE, COMPLETED, UPCOMING
    }

    private static class StepNode extends HBox {
        private final int stepNumber;
        private final StepInfo info;
        private final StackPane circleContainer = new StackPane();
        private final Circle circle = new Circle(14);
        private final Label numberLabel = new Label();
        private final Label titleLabel = new Label();

        StepNode(int stepNumber, StepInfo info) {
            this.stepNumber = stepNumber;
            this.info = info;

            setAlignment(Pos.CENTER_LEFT);
            setSpacing(8);

            numberLabel.setText(String.valueOf(stepNumber));
            numberLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

            circleContainer.getChildren().addAll(circle, numberLabel);

            VBox textBox = new VBox(2);
            textBox.setAlignment(Pos.CENTER_LEFT);
            titleLabel.setText(info.title());
            titleLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 12px;");

            Label descLabel = new Label(info.description());
            descLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");

            textBox.getChildren().addAll(titleLabel, descLabel);

            getChildren().addAll(circleContainer, textBox);
        }

        void setState(StepState state) {
            switch (state) {
                case ACTIVE -> {
                    circle.setStyle("-fx-fill: -color-primary;");
                    numberLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
                    titleLabel.setStyle("-fx-text-fill: -color-primary; -fx-font-weight: bold; -fx-font-size: 12px;");
                }
                case COMPLETED -> {
                    circle.setStyle("-fx-fill: -color-success;");
                    try {
                        FontIcon check = new FontIcon("mdi2c-check");
                        check.setIconSize(12);
                        check.setStyle("-fx-icon-color: white;");
                        circleContainer.getChildren().setAll(circle, check);
                    } catch (Exception ignored) {
                        numberLabel.setText("✓");
                        numberLabel.setStyle("-fx-text-fill: white;");
                        circleContainer.getChildren().setAll(circle, numberLabel);
                    }
                    titleLabel.setStyle("-fx-text-fill: -color-text; -fx-font-weight: 600; -fx-font-size: 12px;");
                }
                case UPCOMING -> {
                    circle.setStyle("-fx-fill: -color-surface-alt; -fx-stroke: -color-border; -fx-stroke-width: 1px;");
                    numberLabel.setText(String.valueOf(stepNumber));
                    numberLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-weight: 600; -fx-font-size: 11px;");
                    circleContainer.getChildren().setAll(circle, numberLabel);
                    titleLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-weight: 500; -fx-font-size: 12px;");
                }
            }
        }
    }
}
