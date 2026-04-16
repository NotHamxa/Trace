package com.logiclab.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class StatusBar extends HBox {
    private Label statusLabel;
    private Label warningLabel;
    private Label modeLabel;

    public StatusBar() {
        setSpacing(10);
        setPadding(new Insets(4, 10, 4, 10));
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: " + Theme.BG_CHROME +
                "; -fx-border-color: " + Theme.BORDER_SOFT + " transparent transparent transparent;");

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        warningLabel = new Label("");
        warningLabel.setStyle("-fx-text-fill: " + Theme.ACCENT_YELLOW + "; -fx-font-size: 11;");

        modeLabel = new Label("DRAW");
        modeLabel.setStyle("-fx-text-fill: " + Theme.ACCENT_BLUE + "; -fx-font-size: 11; -fx-font-weight: bold;");

        getChildren().addAll(statusLabel, spacer, warningLabel, modeLabel);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    public void setWarning(String warning) {
        warningLabel.setText(warning);
    }

    public void setMode(AppMode mode) {
        modeLabel.setText(mode.name());
        String color;
        switch (mode) {
            case SIMULATE: color = Theme.ACCENT_GREEN;  break;
            case TEST:     color = Theme.ACCENT_YELLOW; break;
            default:       color = Theme.ACCENT_BLUE;   break;
        }
        modeLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11; -fx-font-weight: bold;");
    }
}
