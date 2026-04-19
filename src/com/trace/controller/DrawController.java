package com.trace.controller;

import com.trace.model.Breadboard;
import com.trace.model.Circuit;
import com.trace.model.Component;
import com.trace.model.ContactPoint;
import com.trace.model.Pin;
import com.trace.model.Wire;
import com.trace.ui.CanvasView;

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
