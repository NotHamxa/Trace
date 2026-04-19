package com.trace.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class StatusBar extends HBox {
    private final Label statusLabel;
    private final Label warningLabel;
    private final Label modeLabel;

    private final HBox updateBox;
    private final Label updateLabel;
    private final Hyperlink updateAction;
    private final ProgressBar updateProgress;

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

        updateLabel = new Label("");
        updateLabel.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + "; -fx-font-size: 11;");
        updateAction = new Hyperlink("");
        updateAction.setStyle("-fx-text-fill: " + Theme.ACCENT_BLUE + "; -fx-font-size: 11; -fx-padding: 0 4 0 4;");
        updateAction.setBorder(null);
        updateProgress = new ProgressBar(0);
        updateProgress.setPrefWidth(120);
        updateProgress.setPrefHeight(10);
        updateProgress.setVisible(false);
        updateProgress.setManaged(false);
        updateBox = new HBox(6, updateProgress, updateLabel, updateAction);
        updateBox.setAlignment(Pos.CENTER_LEFT);
        updateBox.setVisible(false);
        updateBox.setManaged(false);

        modeLabel = new Label("DRAW");
        modeLabel.setStyle("-fx-text-fill: " + Theme.ACCENT_BLUE + "; -fx-font-size: 11; -fx-font-weight: bold;");

        getChildren().addAll(statusLabel, spacer, updateBox, warningLabel, modeLabel);
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

    /** Show "Update vX.Y available — Install" link. onInstall fires when clicked. */
    public void showUpdateAvailable(String version, Runnable onInstall) {
        Platform.runLater(() -> {
            updateProgress.setVisible(false);
            updateProgress.setManaged(false);
            updateLabel.setText("Update " + version + " available");
            updateAction.setText("Install");
            updateAction.setOnAction(e -> { if (onInstall != null) onInstall.run(); });
            showUpdateBox(true);
        });
    }

    /** Show download progress (0.0–1.0). */
    public void showUpdateProgress(double progress) {
        Platform.runLater(() -> {
            updateProgress.setVisible(true);
            updateProgress.setManaged(true);
            updateProgress.setProgress(progress);
            int pct = (int) Math.round(progress * 100);
            updateLabel.setText("Downloading update " + pct + "%");
            updateAction.setText("");
            updateAction.setOnAction(null);
            showUpdateBox(true);
        });
    }

    /** Show "Restart to update" button. onRestart fires when clicked. */
    public void showUpdateReady(Runnable onRestart) {
        Platform.runLater(() -> {
            updateProgress.setVisible(false);
            updateProgress.setManaged(false);
            updateLabel.setText("Update ready");
            updateAction.setText("Restart & install");
            updateAction.setOnAction(e -> { if (onRestart != null) onRestart.run(); });
            showUpdateBox(true);
        });
    }

    public void clearUpdate() {
        Platform.runLater(() -> showUpdateBox(false));
    }

    private void showUpdateBox(boolean show) {
        updateBox.setVisible(show);
        updateBox.setManaged(show);
    }
}
