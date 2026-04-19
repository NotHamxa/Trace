package com.trace.controller;

import com.trace.model.Circuit;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class FileController {
    private Stage stage;

    public FileController(Stage stage) {
        this.stage = stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Prompts for a save location and writes the circuit. Returns the File
     * written to on success, or null if the user cancelled / the save failed.
     */
    public File save(Circuit circuit) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Circuit");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Trace Files", "*.trc")
        );
        chooser.setInitialFileName(circuit.getName() + ".trc");
        File file = chooser.showSaveDialog(stage);

        if (file != null) {
            try {
                circuit.saveToFile(file);
                circuit.setName(file.getName().replace(".trc", ""));
                circuit.setModified(false);
                return file;
            } catch (IOException e) {
                showError("Failed to save: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Writes the circuit to a known file path without prompting. Used for
     * "Save" (vs "Save As") once the current file is established.
     */
    public boolean saveTo(Circuit circuit, File file) {
        if (file == null) return false;
        try {
            circuit.saveToFile(file);
            circuit.setName(file.getName().replace(".trc", ""));
            circuit.setModified(false);
            return true;
        } catch (IOException e) {
            showError("Failed to save: " + e.getMessage());
            return false;
        }
    }

    /** Prompts for a file and loads it. Returns null on cancel/error. */
    public Circuit load() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Circuit");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Trace Files", "*.trc")
        );
        File file = chooser.showOpenDialog(stage);
        return loadFrom(file);
    }

    /** Loads a specific file without a dialog (e.g. from Recents). */
    public Circuit loadFrom(File file) {
        if (file == null || !file.exists()) return null;
        try {
            Circuit circuit = Circuit.loadFromFile(file);
            circuit.setName(file.getName().replace(".trc", ""));
            // Freshly loaded from disk → clean state.
            circuit.setModified(false);
            return circuit;
        } catch (IOException e) {
            showError("Could not load file: " + e.getMessage());
            return null;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
