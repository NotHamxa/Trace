package com.logiclab.ui;

import com.logiclab.controller.DrawController;
import com.logiclab.controller.FileController;
import com.logiclab.controller.SimulateController;
import com.logiclab.exceptions.CircuitShortException;
import com.logiclab.model.Circuit;
import com.logiclab.util.RecentProjects;
import com.logiclab.util.UndoManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.geometry.Orientation;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class MainWindow {
    private BorderPane root;
    private Circuit circuit;
    private CanvasView canvasView;
    private ToolboxPanel toolboxPanel;
    private PropertiesPanel propertiesPanel;
    private Toolbar toolbar;
    private StatusBar statusBar;

    private DrawController drawController;
    private SimulateController simulateController;
    private FileController fileController;

    private TestTablePanel testTablePanel;
    private SplitPane testSplit;

    private AppMode currentMode = AppMode.DRAW;

    private final UndoManager undoManager = new UndoManager();
    private File currentFile;
    private Runnable onCloseProject;
    private Runnable onSubCircuitSaved;
    private Stage stage;

    private final boolean subCircuitMode;
    private String subCircuitId;
    private String subCircuitName;

    public MainWindow() {
        this(new Circuit(), null, null);
    }

    public MainWindow(Circuit initialCircuit, File sourceFile) {
        this(initialCircuit, sourceFile, null);
    }

    public MainWindow(Circuit initialCircuit, File sourceFile, StatusBar sharedStatusBar) {
        this(initialCircuit, sourceFile, sharedStatusBar, false, null, null);
    }

    /** Sub-circuit editor variant. {@code id}/{@code name} may be null for a new definition. */
    public MainWindow(Circuit initialCircuit, StatusBar sharedStatusBar,
                      String subCircuitId, String subCircuitName) {
        this(initialCircuit, null, sharedStatusBar, true, subCircuitId, subCircuitName);
    }

    private MainWindow(Circuit initialCircuit, File sourceFile, StatusBar sharedStatusBar,
                       boolean subCircuitMode, String subCircuitId, String subCircuitName) {
        circuit = initialCircuit != null ? initialCircuit : new Circuit();
        currentFile = sourceFile;
        this.subCircuitMode = subCircuitMode;
        this.subCircuitId = subCircuitId;
        this.subCircuitName = subCircuitName;

        root = new BorderPane();
        root.setStyle("-fx-background-color: " + Theme.BG_EDITOR + ";");

        // Create UI components
        canvasView = new CanvasView(circuit);
        propertiesPanel = new PropertiesPanel();
        statusBar = sharedStatusBar != null ? sharedStatusBar : new StatusBar();
        toolbar = new Toolbar();

        // Controllers
        drawController = new DrawController(circuit, canvasView);
        simulateController = new SimulateController(circuit);
        fileController = new FileController(null); // Stage set later

        // Toolbox - when component selected, start placement
        toolboxPanel = new ToolboxPanel(
                component -> {
                    if (currentMode == AppMode.DRAW) {
                        drawController.placeComponent(component);
                    }
                },
                color -> {
                    if (currentMode == AppMode.DRAW) {
                        canvasView.setWireToolColor(color);
                    }
                },
                () -> {
                    if (currentMode == AppMode.DRAW) {
                        canvasView.setWireToolColor(null);
                    }
                },
                () -> {
                    if (currentMode == AppMode.DRAW) {
                        canvasView.startBreadboardPlacement();
                    }
                },
                subCircuitMode
        );

        // Canvas callbacks
        canvasView.setOnComponentSelected(comp -> {
            if (comp == null && canvasView.getSelectedWire() != null) {
                propertiesPanel.showWireProperties(canvasView.getSelectedWire());
            } else {
                propertiesPanel.showProperties(comp);
            }
        });
        canvasView.setOnStatusMessage(msg -> statusBar.setStatus(msg));
        canvasView.setOnWarningMessage(msg -> statusBar.setWarning(msg));
        canvasView.setOnToolFinished(() -> toolboxPanel.revertToSelect());
        // Snapshot current circuit before any mutation → undo stack, and mark dirty.
        canvasView.setOnBeforeMutation(this::captureUndo);
        drawController.setOnBeforeMutation(this::captureUndo);
        propertiesPanel.setOnLockToggled(canvasView::setLockOnSelection);
        propertiesPanel.setOnComponentChanged(canvasView::redraw);
        propertiesPanel.setOnTagCommitted(this::captureUndo);
        propertiesPanel.setOnPinSideChanged(canvasView::setPinSideOnSelection);
        propertiesPanel.setOnViewSubCircuit(this::openSubCircuitViewer);

        // Toolbar callbacks
        toolbar.setOnModeChange(this::switchMode);
        toolbar.setOnNew(this::newCircuitWithPrompt);
        toolbar.setOnSave(this::saveCurrent);
        toolbar.setOnLoad(this::loadCircuitWithPrompt);
        toolbar.setOnClear(this::clearCircuitWithPrompt);
        toolbar.setOnUndo(this::doUndo);
        toolbar.setOnRedo(this::doRedo);
        toolbar.setOnCloseProject(this::closeProject);
        toolbar.setSubCircuitMode(subCircuitMode);

        // Layout
        root.setTop(toolbar);
        root.setLeft(toolboxPanel);
        root.setCenter(canvasView);
        root.setRight(propertiesPanel);
        // statusBar is shared via App and lives outside this BorderPane.

        // Keyboard shortcuts
        root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        root.setFocusTraversable(true);

        refreshUndoRedoButtons();
        canvasView.redraw();
    }

    // ---------- Undo / redo ----------

    /** Snapshots the current circuit before a mutation lands. */
    private void captureUndo() {
        undoManager.capture(circuit);
        circuit.setModified(true);
        refreshUndoRedoButtons();
    }

    private void doUndo() {
        Circuit restored = undoManager.undo(circuit);
        if (restored == null) return;
        circuit = restored;
        updateCircuitReferences();
        statusBar.setStatus("Undo");
        refreshUndoRedoButtons();
    }

    private void doRedo() {
        Circuit restored = undoManager.redo(circuit);
        if (restored == null) return;
        circuit = restored;
        updateCircuitReferences();
        statusBar.setStatus("Redo");
        refreshUndoRedoButtons();
    }

    private void refreshUndoRedoButtons() {
        toolbar.setUndoRedoEnabled(undoManager.canUndo(), undoManager.canRedo());
    }

    // ---------- File actions with unsaved-changes prompts ----------

    /** Prompts to save if dirty; returns false if the user cancels. */
    public boolean confirmDiscardChanges() {
        if (!circuit.isModified()) return true;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("This project has unsaved changes");
        alert.setContentText("Save before continuing?");
        ButtonType save = new ButtonType("Save");
        ButtonType discard = new ButtonType("Discard");
        ButtonType cancel = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());
        alert.getButtonTypes().setAll(save, discard, cancel);
        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent() || result.get() == cancel) return false;
        if (result.get() == discard) return true;
        // Save
        return saveCurrent();
    }

    /**
     * Saves to the current file if there is one, otherwise prompts. Returns
     * true iff the save completed (caller uses this to know if it can proceed).
     */
    private boolean saveCurrent() {
        if (subCircuitMode) return saveSubCircuit();
        File target;
        if (currentFile != null) {
            if (!fileController.saveTo(circuit, currentFile)) return false;
            target = currentFile;
        } else {
            target = fileController.save(circuit);
            if (target == null) return false;
            currentFile = target;
        }
        RecentProjects.add(target);
        statusBar.setStatus("Saved: " + target.getName());
        return true;
    }

    /**
     * Saves the current circuit to the sub-circuit library. Prompts for id +
     * name on first save; re-uses them on subsequent saves.
     */
    private boolean saveSubCircuit() {
        boolean hasPort = circuit.getComponents().stream().anyMatch(c ->
                c instanceof com.logiclab.model.ports.InputPort ||
                c instanceof com.logiclab.model.ports.OutputPort);
        if (!hasPort) {
            Alert a = new Alert(Alert.AlertType.WARNING,
                    "Add at least one Input Port or Output Port before saving.",
                    ButtonType.OK);
            a.setHeaderText("No ports defined");
            a.showAndWait();
            return false;
        }

        // Every port must carry a non-default, non-blank label so the outer
        // box has meaningful pin names.
        java.util.List<String> unlabeled = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<String> dupes = new java.util.ArrayList<>();
        for (com.logiclab.model.Component comp : circuit.getComponents()) {
            String label = null;
            String fallback = null;
            if (comp instanceof com.logiclab.model.ports.InputPort ip) {
                label = ip.getPortLabel();
                fallback = "IN";
            } else if (comp instanceof com.logiclab.model.ports.OutputPort op) {
                label = op.getPortLabel();
                fallback = "OUT";
            } else {
                continue;
            }
            if (label == null || label.isBlank() || label.equals(fallback)) {
                unlabeled.add(comp.getName() + " @ (" + (int) comp.getX() + "," + (int) comp.getY() + ")");
            } else if (!seen.add(fallback + ":" + label)) {
                dupes.add(fallback + " \"" + label + "\"");
            }
        }
        if (!unlabeled.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING,
                    "Every Input/Output Port needs a unique label (edit in the Properties panel):\n\n  "
                            + String.join("\n  ", unlabeled),
                    ButtonType.OK);
            a.setHeaderText("Unlabeled ports");
            a.showAndWait();
            return false;
        }
        if (!dupes.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING,
                    "Port labels must be unique within their side (inputs / outputs):\n\n  "
                            + String.join("\n  ", dupes),
                    ButtonType.OK);
            a.setHeaderText("Duplicate port labels");
            a.showAndWait();
            return false;
        }

        if (subCircuitId == null || subCircuitId.isEmpty()) {
            javafx.scene.control.Dialog<javafx.util.Pair<String, String>> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Save Sub-Circuit");
            dialog.setHeaderText("Give this sub-circuit a unique id and a display name.");
            ButtonType saveBtn = new ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

            javafx.scene.control.TextField idField = new javafx.scene.control.TextField();
            idField.setPromptText("user.halfadder.v1");
            javafx.scene.control.TextField nameField = new javafx.scene.control.TextField();
            nameField.setPromptText("Half Adder");

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(8); grid.setVgap(8);
            grid.setPadding(new javafx.geometry.Insets(10));
            grid.add(new javafx.scene.control.Label("Id:"), 0, 0);
            grid.add(idField, 1, 0);
            grid.add(new javafx.scene.control.Label("Name:"), 0, 1);
            grid.add(nameField, 1, 1);
            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(bt -> bt == saveBtn
                    ? new javafx.util.Pair<>(idField.getText().trim(), nameField.getText().trim())
                    : null);

            Optional<javafx.util.Pair<String, String>> result = dialog.showAndWait();
            if (result.isEmpty()) return false;
            String id = result.get().getKey();
            String name = result.get().getValue();
            if (id.isEmpty() || name.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Both id and name are required.", ButtonType.OK).showAndWait();
                return false;
            }
            subCircuitId = id;
            subCircuitName = name;
        }

        try {
            com.logiclab.model.subcircuit.SubCircuitDefinition def =
                    new com.logiclab.model.subcircuit.SubCircuitDefinition(subCircuitId, subCircuitName, circuit);
            com.logiclab.io.SubCircuitLibrary.save(def, System.getProperty("user.name"));
            circuit.setModified(false);
            statusBar.setStatus("Saved sub-circuit: " + subCircuitName);
            if (onSubCircuitSaved != null) onSubCircuitSaved.run();
            return true;
        } catch (java.io.IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save sub-circuit: " + e.getMessage(), ButtonType.OK).showAndWait();
            return false;
        }
    }

    private void newCircuitWithPrompt() {
        if (!confirmDiscardChanges()) return;
        circuit = new Circuit();
        currentFile = null;
        undoManager.clear();
        updateCircuitReferences();
        refreshUndoRedoButtons();
        statusBar.setStatus("New circuit created");
        statusBar.setWarning("");
    }

    private void loadCircuitWithPrompt() {
        if (!confirmDiscardChanges()) return;
        Circuit loaded = fileController.load();
        if (loaded != null) {
            circuit = loaded;
            // We don't know the exact File chosen (FileController.load uses a
            // dialog). Use a two-step: delegate to the public variant.
            undoManager.clear();
            currentFile = null; // save will prompt next time
            updateCircuitReferences();
            refreshUndoRedoButtons();
            statusBar.setStatus("Loaded: " + circuit.getName());
        }
    }

    private void clearCircuitWithPrompt() {
        if (!confirmDiscardChanges()) return;
        circuit = new Circuit();
        currentFile = null;
        undoManager.clear();
        updateCircuitReferences();
        refreshUndoRedoButtons();
        statusBar.setStatus("Circuit cleared");
        statusBar.setWarning("");
    }

    /**
     * Pops up a read-only window showing the internals of a sub-circuit
     * instance. User can pan/zoom but not edit anything.
     */
    private void openSubCircuitViewer(com.logiclab.model.subcircuit.SubCircuitInstance sci) {
        if (sci == null || sci.isBroken() || sci.getDefinition() == null) return;
        Circuit inner = sci.getDefinition().getInner();

        CanvasView viewer = new CanvasView(inner);
        viewer.setViewOnly(true);
        viewer.setMode(AppMode.SIMULATE);

        BorderPane root = new BorderPane(viewer);
        root.setStyle("-fx-background-color: " + Theme.BG_EDITOR + ";");

        Stage dialog = new Stage();
        dialog.setTitle("Subcircuit: " + sci.getDefinition().getName() + "  (read-only)");
        dialog.initOwner(stage);
        dialog.setScene(new javafx.scene.Scene(root, 900, 600));
        dialog.show();
        viewer.redraw();
    }

    private void closeProject() {
        if (!confirmDiscardChanges()) return;
        if (onCloseProject != null) onCloseProject.run();
    }

    // ---------- Mode switching (unchanged logic) ----------

    private void switchMode(AppMode mode) {
        // Tear down test-mode split pane when leaving TEST
        if (currentMode == AppMode.TEST && mode != AppMode.TEST) {
            if (testSplit != null) {
                testSplit.getItems().clear();
                root.setCenter(canvasView);
                testSplit = null;
            }
        }

        currentMode = mode;
        statusBar.setMode(mode);

        if (mode == AppMode.SIMULATE || mode == AppMode.TEST) {
            try {
                List<String> warnings = simulateController.startSimulation();
                if (!warnings.isEmpty()) {
                    statusBar.setWarning("Warnings: " + warnings.size() + " issue(s)");
                    statusBar.setStatus(warnings.get(0));
                } else {
                    statusBar.setWarning("");
                    statusBar.setStatus(mode == AppMode.TEST ? "Test mode" : "Simulation running");
                }
            } catch (CircuitShortException ex) {
                statusBar.setWarning("SHORT CIRCUIT!");
                statusBar.setStatus(ex.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Short Circuit");
                alert.setHeaderText("Short circuit detected!");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
                return;
            } catch (Exception ex) {
                statusBar.setWarning("ERROR");
                statusBar.setStatus(ex.getClass().getSimpleName() + ": " + ex.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Simulation Error");
                alert.setHeaderText("An error occurred during simulation");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
                return;
            }

            if (mode == AppMode.TEST) {
                if (testTablePanel == null) {
                    testTablePanel = new TestTablePanel(circuit, canvasView::redraw);
                } else {
                    testTablePanel.setCircuit(circuit);
                }
                testSplit = new SplitPane(canvasView, testTablePanel);
                testSplit.setOrientation(Orientation.VERTICAL);
                testSplit.setDividerPositions(0.6);
                root.setCenter(testSplit);
            }
        } else {
            statusBar.setWarning("");
            statusBar.setStatus("Draw mode");
        }

        canvasView.setMode(mode == AppMode.TEST ? AppMode.SIMULATE : mode);
    }

    private void updateCircuitReferences() {
        canvasView.setCircuit(circuit);
        drawController.setCircuit(circuit);
        simulateController.setCircuit(circuit);
        propertiesPanel.showProperties(null);
        if (testTablePanel != null) testTablePanel.setCircuit(circuit);
        canvasView.redraw();
    }

    private void handleKeyPress(KeyEvent e) {
        if (e.isControlDown() && e.getCode() == KeyCode.S) {
            saveCurrent();
            e.consume();
        } else if (e.isControlDown() && e.getCode() == KeyCode.O) {
            loadCircuitWithPrompt();
            e.consume();
        } else if (e.isControlDown() && e.getCode() == KeyCode.N) {
            newCircuitWithPrompt();
            e.consume();
        } else if (e.isControlDown() && e.getCode() == KeyCode.Z) {
            doUndo();
            e.consume();
        } else if (e.isControlDown() && e.getCode() == KeyCode.Y) {
            doRedo();
            e.consume();
        } else if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
            if (currentMode == AppMode.DRAW) {
                drawController.deleteSelected();
                propertiesPanel.showProperties(null);
            }
            e.consume();
        } else if (!e.isControlDown() && !e.isAltDown() && !e.isMetaDown()
                && e.getCode() == KeyCode.L) {
            // Lock/unlock the current selection — works in either mode.
            if (canvasView.toggleLockOnSelection()) {
                if (canvasView.getSelectedComponent() != null) {
                    propertiesPanel.showProperties(canvasView.getSelectedComponent());
                } else if (canvasView.getSelectedWire() != null) {
                    propertiesPanel.showWireProperties(canvasView.getSelectedWire());
                }
                e.consume();
            }
        } else if (!e.isControlDown() && !e.isAltDown() && !e.isMetaDown()
                && currentMode == AppMode.DRAW) {
            // Quick tool-swap shortcuts (draw mode only, no modifier keys).
            switch (e.getCode()) {
                case S:
                    toolboxPanel.activateSelect();
                    e.consume();
                    break;
                case R:
                    toolboxPanel.activateRedWire();
                    e.consume();
                    break;
                case B:
                    toolboxPanel.activateBlackWire();
                    e.consume();
                    break;
                case W:
                    toolboxPanel.activateWhiteWire();
                    e.consume();
                    break;
                default:
                    break;
            }
        }
    }

    public BorderPane getRoot() {
        return root;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        fileController.setStage(stage);
    }

    public Stage getStage() { return stage; }

    public void setOnCloseProject(Runnable handler) { this.onCloseProject = handler; }

    public void setOnSubCircuitSaved(Runnable handler) { this.onSubCircuitSaved = handler; }

    public File getCurrentFile() { return currentFile; }
    public void setCurrentFile(File f) { this.currentFile = f; }
}
