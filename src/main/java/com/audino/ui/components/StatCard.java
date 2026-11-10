package com.audino.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Metric dashboard KPI stat card with value, label, icon badge, and trend indicator.
 */
public class StatCard extends VBox {

    private final Label titleLabel = new Label();
    private final Label valueLabel = new Label();
    private final Label subtitleLabel = new Label();
    private final StackPane iconContainer = new StackPane();
    private FontIcon iconNode;

    public StatCard(String title, String initialValue, String subtitle, String iconLiteral, String colorClass) {
        getStyleClass().addAll("surface-card", "stat-card");
        setSpacing(10);
        setPadding(new Insets(16, 20, 16, 20));
        setMinHeight(115);
        VBox.setVgrow(this, Priority.ALWAYS);

        // Header: Title + Icon Badge
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        titleLabel.setText(title.toUpperCase());
        titleLabel.getStyleClass().addAll("text-muted", "stat-title");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 0.5px;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        iconContainer.getStyleClass().addAll("stat-icon-container", colorClass != null ? colorClass : "primary-soft");
        iconContainer.setMinSize(36, 36);
        iconContainer.setMaxSize(36, 36);
        iconContainer.setStyle("-fx-background-radius: 8px; -fx-alignment: center;");

        if (iconLiteral != null && !iconLiteral.isBlank()) {
            try {
                iconNode = new FontIcon(iconLiteral);
                iconNode.setIconSize(18);
                iconContainer.getChildren().add(iconNode);
            } catch (Exception ignored) {
            }
        }

        topRow.getChildren().addAll(titleLabel, iconContainer);

        // Main KPI Value
        valueLabel.setText(initialValue);
        valueLabel.getStyleClass().addAll("stat-value");
        valueLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        // Subtitle / Trend
        subtitleLabel.setText(subtitle != null ? subtitle : "");
        subtitleLabel.getStyleClass().addAll("text-secondary", "stat-subtitle");
        subtitleLabel.setStyle("-fx-font-size: 11px;");

        getChildren().addAll(topRow, valueLabel, subtitleLabel);
    }

    public StatCard(String title, String initialValue, String iconLiteral) {
        this(title, initialValue, null, iconLiteral, "primary-soft");
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }

    public String getValue() {
        return valueLabel.getText();
    }
}
