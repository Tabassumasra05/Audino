package com.audino.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Metric dashboard KPI stat card with value, label, circular icon badge, and subtitle.
 * Sizes naturally to its content and expands equally inside a GridPane or HBox row.
 */
public class StatCard extends VBox {

    private final Label titleLabel = new Label();
    private final Label valueLabel = new Label();
    private final Label subtitleLabel = new Label();
    private final StackPane iconContainer = new StackPane();
    private FontIcon iconNode;

    public StatCard(String title, String initialValue, String subtitle, String iconLiteral, String colorClass) {
        getStyleClass().addAll("surface-card", "stat-card");
        setSpacing(8);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_LEFT);
        setMinHeight(Region.USE_PREF_SIZE);
        setPrefHeight(Region.USE_COMPUTED_SIZE);
        setMaxHeight(Double.MAX_VALUE);
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);

        // Top row: Title (left) + Icon in a soft-colored circle (right)
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        titleLabel.setText(title.toUpperCase());
        titleLabel.getStyleClass().addAll("text-muted", "stat-title");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 0.5px;");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        titleLabel.setMinHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        iconContainer.getStyleClass().addAll("stat-icon-container", colorClass != null ? colorClass : "primary-soft");
        iconContainer.setMinSize(38, 38);
        iconContainer.setPrefSize(38, 38);
        iconContainer.setMaxSize(38, 38);

        if (iconLiteral != null && !iconLiteral.isBlank()) {
            try {
                iconNode = new FontIcon(iconLiteral);
                iconNode.setIconSize(18);
                iconContainer.getChildren().add(iconNode);
            } catch (Exception ignored) {
            }
        }

        topRow.getChildren().addAll(titleLabel, iconContainer);

        // Big KPI Value
        valueLabel.setText(initialValue);
        valueLabel.getStyleClass().addAll("stat-value");
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: -color-text;");
        valueLabel.setMinHeight(Region.USE_PREF_SIZE);

        // Subtitle
        subtitleLabel.setText(subtitle != null ? subtitle : "");
        subtitleLabel.getStyleClass().addAll("text-secondary", "stat-subtitle");
        subtitleLabel.setStyle("-fx-font-size: 12px;");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMinHeight(Region.USE_PREF_SIZE);

        getChildren().addAll(topRow, valueLabel, subtitleLabel);
    }

    public StatCard(String title, String initialValue, String iconLiteral) {
        this(title, initialValue, null, iconLiteral, "primary-soft");
    }

    /**
     * Populates a GridPane row with equal-width StatCards filling 100% of the available width.
     */
    public static void populateRow(GridPane grid, StatCard... cards) {
        if (grid == null || cards == null || cards.length == 0) return;

        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setMinHeight(Region.USE_PREF_SIZE);

        double pct = 100.0 / cards.length;
        for (int i = 0; i < cards.length; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(pct);
            col.setHgrow(Priority.ALWAYS);
            col.setFillWidth(true);
            grid.getColumnConstraints().add(col);

            StatCard card = cards[i];
            GridPane.setColumnIndex(card, i);
            GridPane.setRowIndex(card, 0);
            GridPane.setHgrow(card, Priority.ALWAYS);
            GridPane.setFillWidth(card, true);
            GridPane.setFillHeight(card, true);
            grid.getChildren().add(card);
        }
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
