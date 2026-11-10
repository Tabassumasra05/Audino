package com.audino.ui.components;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

/**
 * Enhanced search input with embedded icon, clear button, and debounced search triggers.
 */
public class SearchField extends HBox {

    private final TextField textField = new TextField();
    private final Button clearBtn = new Button();
    private final PauseTransition debounceTimer = new PauseTransition(Duration.millis(250));
    private Consumer<String> onSearch;

    public SearchField(String promptText) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        getStyleClass().addAll("search-field-container");
        setStyle("-fx-background-color: -color-surface; -fx-border-color: -color-border; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 2px 8px;");

        // Magnifying glass icon
        try {
            FontIcon searchIcon = new FontIcon("mdi2m-magnify");
            searchIcon.setIconSize(16);
            searchIcon.setStyle("-fx-icon-color: -color-text-muted;");
            getChildren().add(searchIcon);
        } catch (Exception ignored) {
        }

        // Inner text field
        textField.setPromptText(promptText);
        textField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 6px 4px;");
        HBox.setHgrow(textField, Priority.ALWAYS);
        getChildren().add(textField);

        // Clear button
        try {
            FontIcon closeIcon = new FontIcon("mdi2c-close");
            closeIcon.setIconSize(14);
            clearBtn.setGraphic(closeIcon);
        } catch (Exception ignored) {
            clearBtn.setText("×");
        }
        clearBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2px 4px;");
        clearBtn.setVisible(false);
        clearBtn.setOnAction(e -> {
            textField.clear();
            if (onSearch != null) onSearch.accept("");
        });
        getChildren().add(clearBtn);

        // Listeners
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            clearBtn.setVisible(newVal != null && !newVal.isEmpty());
            debounceTimer.setOnFinished(e -> {
                if (onSearch != null) {
                    onSearch.accept(newVal);
                }
            });
            debounceTimer.playFromStart();
        });
    }

    public SearchField() {
        this("Search...");
    }

    public String getPromptText() {
        return textField.getPromptText();
    }

    public void setPromptText(String promptText) {
        textField.setPromptText(promptText);
    }

    public void setOnSearch(Consumer<String> onSearch) {
        this.onSearch = onSearch;
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public TextField getTextField() {
        return textField;
    }

    public void requestSearchFocus() {
        textField.requestFocus();
    }
}
