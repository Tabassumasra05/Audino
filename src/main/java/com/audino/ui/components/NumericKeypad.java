package com.audino.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * On-screen touch numeric keypad for patient kiosk verification.
 */
public class NumericKeypad extends GridPane {

    private TextField targetField;

    public NumericKeypad(TextField targetField) {
        this.targetField = targetField;

        setAlignment(Pos.CENTER);
        setHgap(10);
        setVgap(10);
        setStyle("-fx-padding: 10px;");

        // 1 to 9 buttons
        int num = 1;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                final String digit = String.valueOf(num++);
                Button btn = createKeypadButton(digit);
                btn.setOnAction(e -> appendText(digit));
                add(btn, c, r);
            }
        }

        // Row 4: Clear, 0, Backspace
        Button clearBtn = createKeypadButton("C");
        clearBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: -color-surface-alt; -fx-border-color: -color-border; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-min-width: 65px; -fx-min-height: 55px; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> {
            if (this.targetField != null) {
                this.targetField.clear();
            }
        });
        add(clearBtn, 0, 3);

        Button zeroBtn = createKeypadButton("0");
        zeroBtn.setOnAction(e -> appendText("0"));
        add(zeroBtn, 1, 3);

        Button backBtn = createKeypadButton("");
        try {
            FontIcon backIcon = new FontIcon("mdi2b-backspace-outline");
            backIcon.setIconSize(20);
            backBtn.setGraphic(backIcon);
        } catch (Exception ignored) {
            backBtn.setText("←");
        }
        backBtn.setStyle("-fx-font-size: 16px; -fx-background-color: -color-surface-alt; -fx-border-color: -color-border; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-min-width: 65px; -fx-min-height: 55px; -fx-cursor: hand;");
        backBtn.setOnAction(e -> {
            if (this.targetField != null) {
                String cur = this.targetField.getText();
                if (cur != null && !cur.isEmpty()) {
                    this.targetField.setText(cur.substring(0, cur.length() - 1));
                }
            }
        });
        add(backBtn, 2, 3);
    }

    public void setTargetField(TextField targetField) {
        this.targetField = targetField;
    }

    private Button createKeypadButton(String label) {
        Button btn = new Button(label);
        btn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: -color-surface; -fx-border-color: -color-border; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-min-width: 65px; -fx-min-height: 55px; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: -color-primary-soft; -fx-border-color: -color-primary; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-min-width: 65px; -fx-min-height: 55px; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-color: -color-surface; -fx-border-color: -color-border; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-min-width: 65px; -fx-min-height: 55px; -fx-cursor: hand;"));
        return btn;
    }

    private void appendText(String str) {
        if (targetField != null) {
            targetField.appendText(str);
        }
    }
}
