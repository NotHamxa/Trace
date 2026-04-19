package com.trace.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

public class Toolbar extends HBox {
    private ToggleButton drawBtn;
    private ToggleButton simBtn;
    private ToggleButton testBtn;
    private Button newBtn, saveBtn, loadBtn, clearBtn;
    private Button undoBtn, redoBtn, menuBtn;
    private Consumer<AppMode> onModeChange;
    private Runnable onNew, onSave, onLoad, onClear;
    private Runnable onUndo, onRedo, onCloseProject;

    public Toolbar() {
        setSpacing(8);
        setPadding(new Insets(6, 10, 6, 10));
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: " + Theme.BG_CHROME +
                "; -fx-border-color: transparent transparent " + Theme.BORDER_SOFT + " transparent;");

        // Mode toggles
        ToggleGroup modeGroup = new ToggleGroup();

        drawBtn = new ToggleButton("Draw Mode");
        drawBtn.setToggleGroup(modeGroup);
        drawBtn.setSelected(true);
        styleToggle(drawBtn, true);
        drawBtn.setOnAction(e -> {
            if (onModeChange != null) onModeChange.accept(AppMode.DRAW);
            styleToggle(drawBtn, true);
            styleToggle(simBtn, false);
            styleToggle(testBtn, false);
        });

        simBtn = new ToggleButton("Simulate");
        simBtn.setToggleGroup(modeGroup);
        styleToggle(simBtn, false);
        simBtn.setOnAction(e -> {
            if (onModeChange != null) onModeChange.accept(AppMode.SIMULATE);
            styleToggle(simBtn, true);
            styleToggle(drawBtn, false);
            styleToggle(testBtn, false);
        });

        testBtn = new ToggleButton("Test");
        testBtn.setToggleGroup(modeGroup);
        styleToggle(testBtn, false);
        testBtn.setOnAction(e -> {
            if (onModeChange != null) onModeChange.accept(AppMode.TEST);
            styleToggle(testBtn, true);
            styleToggle(drawBtn, false);
            styleToggle(simBtn, false);
        });

        Separator sep1 = new Separator();
        sep1.setOrientation(javafx.geometry.Orientation.VERTICAL);

        // File operations
        newBtn = createButton("New");
        saveBtn = createButton("Save");
        loadBtn = createButton("Load");

        Separator sep2 = new Separator();
        sep2.setOrientation(javafx.geometry.Orientation.VERTICAL);

        // Undo / Redo
        undoBtn = createButton("Undo");
        redoBtn = createButton("Redo");
        undoBtn.setDisable(true);
        redoBtn.setDisable(true);

        Separator sep3 = new Separator();
        sep3.setOrientation(javafx.geometry.Orientation.VERTICAL);

        clearBtn = createButton("Clear All");

        Separator sep4 = new Separator();
        sep4.setOrientation(javafx.geometry.Orientation.VERTICAL);

        menuBtn = createButton("Menu");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Title
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("Trace");
        titleLabel.setStyle("-fx-text-fill: " + Theme.TEXT_SECONDARY +
                "; -fx-font-size: 13; -fx-font-style: italic;");

        getChildren().addAll(drawBtn, simBtn, testBtn, sep1, newBtn, saveBtn, loadBtn,
                sep2, undoBtn, redoBtn, sep3, clearBtn, sep4, menuBtn, spacer, titleLabel);

        newBtn.setOnAction(e -> { if (onNew != null) onNew.run(); });
        saveBtn.setOnAction(e -> { if (onSave != null) onSave.run(); });
        loadBtn.setOnAction(e -> { if (onLoad != null) onLoad.run(); });
        clearBtn.setOnAction(e -> { if (onClear != null) onClear.run(); });
        undoBtn.setOnAction(e -> { if (onUndo != null) onUndo.run(); });
        redoBtn.setOnAction(e -> { if (onRedo != null) onRedo.run(); });
        menuBtn.setOnAction(e -> { if (onCloseProject != null) onCloseProject.run(); });
    }

    /**
     * Reshapes the toolbar for sub-circuit editing: hides the Load and
     * "Save as Sub-Circuit" buttons (both meaningless in that mode).
     */
    public void setSubCircuitMode(boolean subCircuit) {
        loadBtn.setVisible(!subCircuit);
        loadBtn.setManaged(!subCircuit);
        simBtn.setVisible(!subCircuit);
        simBtn.setManaged(!subCircuit);
    }

    /** Enables/disables the undo/redo buttons (host should call this on state change). */
    public void setUndoRedoEnabled(boolean canUndo, boolean canRedo) {
        undoBtn.setDisable(!canUndo);
        redoBtn.setDisable(!canRedo);
    }

    private Button createButton(String text) {
        Button btn = new Button(text);
        String idle = "-fx-background-color: " + Theme.BTN_BG + "; -fx-text-fill: " + Theme.TEXT_PRIMARY +
                "; -fx-font-size: 11; -fx-background-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;";
        String hover = "-fx-background-color: " + Theme.BTN_HOVER + "; -fx-text-fill: #ffffff; " +
                "-fx-font-size: 11; -fx-background-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;";
        btn.setStyle(idle);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(idle));
        return btn;
    }

    private void styleToggle(ToggleButton btn, boolean active) {
        if (active) {
            btn.setStyle(
                    "-fx-background-color: " + Theme.ACCENT_BLUE + "; -fx-text-fill: white; " +
                    "-fx-font-size: 11; -fx-background-radius: 6; -fx-padding: 5 14; -fx-font-weight: bold;"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: " + Theme.BTN_BG + "; -fx-text-fill: " + Theme.TEXT_PRIMARY +
                    "; -fx-font-size: 11; -fx-background-radius: 6; -fx-padding: 5 14;"
            );
        }
    }

    public void setOnModeChange(Consumer<AppMode> handler) { this.onModeChange = handler; }
    public void setOnNew(Runnable handler) { this.onNew = handler; }
    public void setOnSave(Runnable handler) { this.onSave = handler; }
    public void setOnLoad(Runnable handler) { this.onLoad = handler; }
    public void setOnClear(Runnable handler) { this.onClear = handler; }
    public void setOnUndo(Runnable handler) { this.onUndo = handler; }
    public void setOnRedo(Runnable handler) { this.onRedo = handler; }
    public void setOnCloseProject(Runnable handler) { this.onCloseProject = handler; }
}
