package com.audino.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Transient notification banner floating over the active scene.
 */
public class Toast extends HBox {

    public enum Type {
        SUCCESS("mdi2c-check-circle", "#16A34A", "#DCFCE7", "#166534"),
        ERROR("mdi2a-alert-circle", "#DC2626", "#FEE2E2", "#991B1B"),
        WARNING("mdi2a-alert", "#D97706", "#FEF3C7", "#92400E"),
        INFO("mdi2i-information", "#2563EB", "#DBEAFE", "#1E40AF");

        final String icon;
        final String accentColor;
        final String bgColor;
        final String textColor;

        Type(String icon, String accentColor, String bgColor, String textColor) {
            this.icon = icon;
            this.accentColor = accentColor;
            this.bgColor = bgColor;
            this.textColor = textColor;
        }
    }

    public Toast(String message, Type type) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setPadding(new Insets(10, 18, 10, 18));
        setMaxWidth(480);
        setStyle("-fx-background-color: " + type.bgColor + "; -fx-border-color: " + type.accentColor + "; -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        try {
            FontIcon icon = new FontIcon(type.icon);
            icon.setIconSize(18);
            icon.setStyle("-fx-icon-color: " + type.accentColor + ";");
            getChildren().add(icon);
        } catch (Exception ignored) {
        }

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-weight: 500; -fx-font-size: 13px; -fx-text-fill: " + type.textColor + ";");
        HBox.setHgrow(msgLabel, Priority.ALWAYS);
        getChildren().add(msgLabel);
    }

    public static void show(Pane container, String message, Type type) {
        if (container == null) return;

        Toast toast = new Toast(message, type);
        toast.setOpacity(0.0);

        if (container instanceof StackPane sp) {
            StackPane.setAlignment(toast, Pos.TOP_RIGHT);
            StackPane.setMargin(toast, new Insets(20, 20, 0, 0));
        }

        container.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toast);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition stay = new PauseTransition(Duration.seconds(3.5));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> container.getChildren().remove(toast));

        SequentialTransition sequence = new SequentialTransition(fadeIn, stay, fadeOut);
        sequence.play();
    }
}
