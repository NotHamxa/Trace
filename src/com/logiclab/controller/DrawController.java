package com.logiclab.controller;

import com.logiclab.model.Breadboard;
import com.logiclab.model.Circuit;
import com.logiclab.model.Component;
import com.logiclab.model.ContactPoint;
import com.logiclab.model.Pin;
import com.logiclab.model.Wire;
import com.logiclab.ui.CanvasView;

public class DrawController {
    private Circuit circuit;
    private CanvasView canvasView;
    private Runnable onBeforeMutation;

    public DrawController(Circuit circuit, CanvasView canvasView) {
        this.circuit = circuit;
        this.canvasView = canvasView;
    }

    public void placeComponent(Component component) {
        canvasView.startPlacement(component);
    }

    public void deleteSelected() {
        Wire selectedWire = canvasView.getSelectedWire();
        if (selectedWire != null) {
            if (onBeforeMutation != null) onBeforeMutation.run();
            circuit.removeWire(selectedWire);
            canvasView.clearWireSelection();
            canvasView.redraw();
            return;
        }
        Component selected = canvasView.getSelectedComponent();
        if (selected != null) {
            if (onBeforeMutation != null) onBeforeMutation.run();
            // Free any breadboard holes this component occupied
            for (Pin p : selected.getPins()) {
                for (Breadboard bb : circuit.getBreadboards()) {
                    for (ContactPoint cp : bb.getAllContactPoints()) {
                        if (cp.getOccupant() == p) cp.removePin();
                    }
                }
            }
            circuit.removeComponent(selected);
            canvasView.redraw();
        }
    }

    public void setCircuit(Circuit circuit) {
        this.circuit = circuit;
    }

    public void setOnBeforeMutation(Runnable handler) {
        this.onBeforeMutation = handler;
    }
}
