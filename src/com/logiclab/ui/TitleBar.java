package com.logiclab.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.function.BooleanSupplier;

/**
 * Custom window chrome drawn inside the scene so we can theme it. The native
 * title bar is hidden via {@code StageStyle.UNDECORATED} in App.
 */
public class TitleBar extends HBox {
    private static final double HEIGHT = 34;

    private final Stage stage;
    private final Label titleLabel;
    private BooleanSupplier beforeClose;

    private double dragOffsetX;
    private double dragOffsetY;

    public TitleBar(Stage stage, BooleanSupplier beforeClose) {
        this.stage = stage;
        this.beforeClose = beforeClose;

        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 0, 0, 10));
        setSpacing(8);
        setPrefHeight(HEIGHT);
        setMinHeight(HEIGHT);
        setMaxHeight(HEIGHT);
        setStyle("-fx-background-color: " + Theme.BG_CHROME + ";" +
                "-fx-border-color: transparent transparent " + Theme.BORDER_STRONG + " transparent;");

        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/icons/icon.png"));
            logoView = new ImageView(logo);
            logoView.setFitWidth(22);
            logoView.setFitHeight(22);
            logoView.setSmooth(true);
            logoView.setPreserveRatio(true);
        } catch (Exception e) {
            // Icon missing — title bar drops the badge.
        }

        titleLabel = new Label("LogicLab");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        titleLabel.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane minBtn = buildWindowButton("\u2013", false); // en dash (minimize)
        minBtn.setOnMouseClicked(e -> stage.setIconified(true));

        StackPane maxBtn = buildWindowButton("\u25A1", false); // square (maximize)
        maxBtn.setOnMouseClicked(e -> stage.setMaximized(!stage.isMaximized()));

        StackPane closeBtn = buildWindowButton("\u2715", true); // cross (close)
        closeBtn.setOnMouseClicked(e -> requestClose());

        if (logoView != null) {
            getChildren().add(logoView);
        }
        getChildren().addAll(titleLabel, spacer, minBtn, maxBtn, closeBtn);

        // Drag to move the window (except when maximized)
        setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });
        setOnMouseDragged(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            if (stage.isMaximized()) return;
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
        // Double-click toggles maximize
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }

    private StackPane buildWindowButton(String glyph, boolean isClose) {
        Label lbl = new Label(glyph);
        lbl.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        lbl.setStyle("-fx-text-fill: " + Theme.TEXT_PRIMARY + ";");

        StackPane btn = new StackPane(lbl);
        btn.setPrefSize(46, HEIGHT);
        btn.setMinSize(46, HEIGHT);
        btn.setMaxSize(46, HEIGHT);
        String base = "-fx-background-color: transparent; -fx-cursor: hand;";
        String hover = "-fx-background-color: " + (isClose ? "#c42b1c" : Theme.BG_HOVER) +
                "; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private void requestClose() {
        if (beforeClose == null || beforeClose.getAsBoolean()) {
            stage.close();
        }
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setBeforeClose(BooleanSupplier bc) {
        this.beforeClose = bc;
    }
}
