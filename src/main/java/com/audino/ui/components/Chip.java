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
    private Variant variant = Variant.DEFAULT;
    private String iconLiteral;

    public Chip() {
        this("", Variant.DEFAULT, null);
    }

    public Chip(String text, Variant variant, String iconLiteral) {
        this.variant = variant != null ? variant : Variant.DEFAULT;
        this.iconLiteral = iconLiteral;

        setAlignment(Pos.CENTER);
        setSpacing(5);
        updateStyles();

        textLabel.setText(text != null ? text : "");
        textLabel.getStyleClass().add("chip-label");
        getChildren().add(textLabel);

        if (iconLiteral != null && !iconLiteral.isBlank()) {
            updateIcon();
        }
    }

    public Chip(String text, Variant variant) {
        this(text, variant, null);
    }

    public Chip(String text) {
        this(text, Variant.DEFAULT, null);
    }

    public void setText(String text) {
        textLabel.setText(text != null ? text : "");
    }

    public String getText() {
        return textLabel.getText();
    }

    public Variant getVariant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        this.variant = variant != null ? variant : Variant.DEFAULT;
        updateStyles();
    }

    public String getIconLiteral() {
        return iconLiteral;
    }

    public void setIconLiteral(String iconLiteral) {
        this.iconLiteral = iconLiteral;
        updateIcon();
    }

    private void updateStyles() {
        getStyleClass().setAll(variant.getStyleClass().split(" "));
    }

    private void updateIcon() {
        if (fontIcon != null) {
            getChildren().remove(fontIcon);
            fontIcon = null;
        }
        if (iconLiteral != null && !iconLiteral.isBlank()) {
            try {
                fontIcon = new FontIcon(iconLiteral);
                fontIcon.setIconSize(12);
                fontIcon.getStyleClass().add("chip-icon");
                int labelIndex = getChildren().indexOf(textLabel);
                if (labelIndex >= 0) {
                    getChildren().add(labelIndex, fontIcon);
                } else {
                    getChildren().add(fontIcon);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
