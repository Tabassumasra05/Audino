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

        // Remove parenthetical notes like (Admin), (Doctor), etc.
        String cleaned = name.replaceAll("(?i)\\s*\\(.*?\\)", "").trim();

        // Remove common honorific titles
        cleaned = cleaned.replaceAll("(?i)^(dr\\.?|mr\\.?|mrs\\.?|ms\\.?|prof\\.?|er\\.?)\\s+", "").trim();

        // Split on whitespace or non-alphabetic separators
        String[] rawParts = cleaned.split("[\\s_\\-\\.]+");
        java.util.List<String> words = new java.util.ArrayList<>();
        for (String p : rawParts) {
            String word = p.replaceAll("[^a-zA-Z]", "");
            if (!word.isEmpty()) {
                words.add(word);
            }
        }

        if (words.isEmpty()) {
            return "?";
        }
        if (words.size() == 1) {
            String w = words.get(0);
            return (w.length() >= 2 ? w.substring(0, 2) : w).toUpperCase();
        }
        return ("" + words.get(0).charAt(0) + words.get(words.size() - 1).charAt(0)).toUpperCase();
    }
}
