package com.audino.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Reusable rounded pill badge component for statuses, alert levels, and roles.
 */
public class Chip extends HBox {

    public enum Variant {
        DEFAULT("chip"),
        PRIMARY("chip chip-primary"),
        SUCCESS("chip chip-success"),
        WARNING("chip chip-warning"),
        DANGER("chip chip-critical"),
        INFO("chip chip-info");

        private final String styleClass;

        Variant(String styleClass) {
            this.styleClass = styleClass;
        }

        public String getStyleClass() {
            return styleClass;
        }
    }

    private final Label textLabel = new Label();
    private FontIcon fontIcon;

    public Chip(String text, Variant variant, String iconLiteral) {
        setAlignment(Pos.CENTER);
        setSpacing(5);
        getStyleClass().addAll(variant.getStyleClass().split(" "));

        if (iconLiteral != null && !iconLiteral.isBlank()) {
            try {
                fontIcon = new FontIcon(iconLiteral);
                fontIcon.setIconSize(12);
                fontIcon.getStyleClass().add("chip-icon");
                getChildren().add(fontIcon);
            } catch (Exception ignored) {
            }
        }

        textLabel.setText(text);
        textLabel.getStyleClass().add("chip-label");
        getChildren().add(textLabel);
    }

    public Chip(String text, Variant variant) {
        this(text, variant, null);
    }

    public Chip(String text) {
        this(text, Variant.DEFAULT, null);
    }

    public void setText(String text) {
        textLabel.setText(text);
    }

    public String getText() {
        return textLabel.getText();
    }
}
