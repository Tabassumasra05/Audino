package com.audino.ui.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Standard page header with title, subtitle, and right-aligned action buttons container.
 */
public class PageHeader extends HBox {

    private final Label titleLabel = new Label();
    private final Label subtitleLabel = new Label();
    private final HBox actionsBox = new HBox(10);

    public PageHeader(String title, String subtitle) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        getStyleClass().add("page-header");

        VBox titleBox = new VBox(2);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        titleLabel.setText(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -color-text;");

        subtitleLabel.setText(subtitle != null ? subtitle : "");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-secondary;");

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(titleBox, actionsBox);
    }

    public PageHeader(String title) {
        this(title, null);
    }

    public void addAction(Node actionNode) {
        actionsBox.getChildren().add(actionNode);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }
}
