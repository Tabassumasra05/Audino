package com.audino.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Clean empty state placeholder displayed when lists, tables, or search queries contain zero items.
 */
public class EmptyState extends VBox {

    public EmptyState(String iconLiteral, String title, String subtitle, String actionButtonText, Runnable onAction) {
        setAlignment(Pos.CENTER);
        setSpacing(12);
        setPadding(new Insets(40, 20, 40, 20));
        getStyleClass().add("empty-state");

        if (iconLiteral != null && !iconLiteral.isBlank()) {
            try {
                FontIcon icon = new FontIcon(iconLiteral);
                icon.setIconSize(48);
                icon.setStyle("-fx-icon-color: -color-text-muted;");
                getChildren().add(icon);
            } catch (Exception ignored) {
            }
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text;");
        getChildren().add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            Label subLabel = new Label(subtitle);
            subLabel.setWrapText(true);
            subLabel.setMaxWidth(400);
            subLabel.setAlignment(Pos.CENTER);
            subLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-secondary; -fx-text-alignment: center;");
            getChildren().add(subLabel);
        }

        if (actionButtonText != null && onAction != null) {
            Button btn = new Button(actionButtonText);
            btn.getStyleClass().add("btn-primary");
            btn.setOnAction(e -> onAction.run());
            getChildren().add(btn);
        }
    }

    public EmptyState(String iconLiteral, String title, String subtitle) {
        this(iconLiteral, title, subtitle, null, null);
    }
}
