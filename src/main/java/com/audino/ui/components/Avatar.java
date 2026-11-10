package com.audino.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

/**
 * Circular avatar displaying patient or practitioner initials with clean background styling.
 */
public class Avatar extends StackPane {

    public Avatar(String name, double radius, String bgColor, String textColor) {
        setPrefSize(radius * 2, radius * 2);
        setMaxSize(radius * 2, radius * 2);
        setMinSize(radius * 2, radius * 2);
        setAlignment(Pos.CENTER);

        Circle circle = new Circle(radius);
        circle.setStyle("-fx-fill: " + (bgColor != null ? bgColor : "#0F766E") + ";");

        String initials = computeInitials(name);
        Label label = new Label(initials);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: " + Math.max(10, (int) (radius * 0.8)) + "px; -fx-text-fill: " + (textColor != null ? textColor : "#FFFFFF") + ";");

        getChildren().addAll(circle, label);
    }

    public Avatar(String name, double radius) {
        this(name, radius, "#0F766E", "#FFFFFF");
    }

    public static String computeInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
