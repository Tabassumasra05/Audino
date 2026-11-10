package com.audino.ui.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * Chip-based tag input component allowing interactive addition and removal of clinical items.
 */
public class TagInput extends FlowPane {

    private final ObservableList<String> tags = FXCollections.observableArrayList();
    private final TextField inputField = new TextField();
    private final Chip.Variant chipVariant;

    public TagInput(String placeholder, Chip.Variant variant) {
        this.chipVariant = variant != null ? variant : Chip.Variant.DEFAULT;
        setHgap(6);
        setVgap(6);
        setPadding(new Insets(6));
        setStyle("-fx-background-color: -color-surface; -fx-border-color: -color-border; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-min-height: 40px;");

        inputField.setPromptText(placeholder);
        inputField.setPrefWidth(140);
        inputField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 4px;");

        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.COMMA) {
                e.consume();
                addCurrentInput();
            } else if (e.getCode() == KeyCode.BACK_SPACE && inputField.getText().isEmpty() && !tags.isEmpty()) {
                tags.remove(tags.size() - 1);
                rebuildChips();
            }
        });

        getChildren().add(inputField);
    }

    public TagInput(String placeholder) {
        this(placeholder, Chip.Variant.DEFAULT);
    }

    private void addCurrentInput() {
        String text = inputField.getText();
        if (text != null) {
            text = text.replace(",", "").trim();
            if (!text.isEmpty() && !tags.contains(text)) {
                tags.add(text);
                inputField.clear();
                rebuildChips();
            }
        }
    }

    public void addTag(String tag) {
        if (tag != null && !tag.isBlank() && !tags.contains(tag.trim())) {
            tags.add(tag.trim());
            rebuildChips();
        }
    }

    public void removeTag(String tag) {
        if (tags.remove(tag)) {
            rebuildChips();
        }
    }

    public void clearTags() {
        tags.clear();
        rebuildChips();
    }

    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    private void rebuildChips() {
        getChildren().clear();
        for (String tag : tags) {
            HBox chip = createTagChip(tag);
            getChildren().add(chip);
        }
        getChildren().add(inputField);
    }

    private HBox createTagChip(String tag) {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(3, 8, 3, 8));
        box.getStyleClass().addAll(chipVariant.getStyleClass().split(" "));

        Label label = new Label(tag);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 600;");

        Button removeBtn = new Button();
        removeBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
        try {
            FontIcon closeIcon = new FontIcon("mdi2c-close");
            closeIcon.setIconSize(12);
            removeBtn.setGraphic(closeIcon);
        } catch (Exception ignored) {
            removeBtn.setText("×");
        }
        removeBtn.setOnAction(e -> {
            tags.remove(tag);
            rebuildChips();
        });

        box.getChildren().addAll(label, removeBtn);
        return box;
    }
}
