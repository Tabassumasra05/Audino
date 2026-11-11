package com.audino.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
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
    private final List<Region> connectors = new ArrayList<>();

    public Stepper(List<StepInfo> steps) {
        this.steps = steps;
        setAlignment(Pos.TOP_CENTER);
        setSpacing(6);
        setMaxWidth(Double.MAX_VALUE);
        setMinHeight(Region.USE_PREF_SIZE);
        getStyleClass().add("stepper-container");

        for (int i = 0; i < steps.size(); i++) {
            StepNode node = new StepNode(i + 1, steps.get(i));
            stepNodes.add(node);
            getChildren().add(node);

            // Add flexible connector line aligned with step circle center
            if (i < steps.size() - 1) {
                Region line = new Region();
                line.setMinWidth(14);
                line.setPrefWidth(28);
                line.setMinHeight(2);
                line.setPrefHeight(2);
                line.setMaxHeight(2);
                line.setStyle("-fx-background-color: -color-border;");

                VBox lineWrapper = new VBox(line);
                lineWrapper.setAlignment(Pos.TOP_CENTER);
                lineWrapper.setPadding(new Insets(13, 0, 0, 0));
                HBox.setHgrow(lineWrapper, Priority.ALWAYS);

                connectors.add(line);
                getChildren().add(lineWrapper);
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
        for (int i = 0; i < connectors.size(); i++) {
            connectors.get(i).setStyle(i < currentStep
                    ? "-fx-background-color: -color-success;"
                    : "-fx-background-color: -color-border;");
        }
    }

    public enum StepState {
        ACTIVE, COMPLETED, UPCOMING
    }

    private static class StepNode extends VBox {
        private final int stepNumber;
        private final StackPane circleContainer = new StackPane();
        private final Circle circle = new Circle(14);
        private final Label numberLabel = new Label();
        private final Label titleLabel = new Label();

        StepNode(int stepNumber, StepInfo info) {
            this.stepNumber = stepNumber;

            setAlignment(Pos.TOP_CENTER);
            setSpacing(5);
            setMinWidth(Region.USE_PREF_SIZE);

            numberLabel.setText(String.valueOf(stepNumber));
            numberLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

            circleContainer.setMinSize(28, 28);
            circleContainer.setPrefSize(28, 28);
            circleContainer.setMaxSize(28, 28);
            circleContainer.getChildren().addAll(circle, numberLabel);

            titleLabel.setText(info.title());
            titleLabel.setMinWidth(Region.USE_PREF_SIZE);
            titleLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 12px;");

            getChildren().addAll(circleContainer, titleLabel);
        }

        void setState(StepState state) {
            switch (state) {
                case ACTIVE -> {
                    circle.setStyle("-fx-fill: -color-primary;");
                    numberLabel.setText(String.valueOf(stepNumber));
                    numberLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
                    circleContainer.getChildren().setAll(circle, numberLabel);
                    titleLabel.setStyle("-fx-text-fill: -color-primary; -fx-font-weight: bold; -fx-font-size: 12px;");
                }
                case COMPLETED -> {
                    circle.setStyle("-fx-fill: -color-success;");
                    try {
                        FontIcon check = new FontIcon("mdi2c-check");
                        check.setIconSize(13);
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
