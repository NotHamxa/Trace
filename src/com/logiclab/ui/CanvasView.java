package com.logiclab.ui;

import com.logiclab.exceptions.InvalidConnectionException;
import com.logiclab.exceptions.OscillationException;
import com.logiclab.interfaces.Interactable;
import com.logiclab.model.*;
import com.logiclab.model.input.DIPSwitch;
import com.logiclab.model.input.PushButton;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.function.Consumer;

public class CanvasView extends Pane {
    private Canvas canvas;
    private GraphicsContext gc;
    private Circuit circuit;
    private AppMode mode = AppMode.DRAW;

    // Drawing state
    private Component selectedComponent;
    private Wire selectedWire;
    private Breadboard selectedBreadboard;
    private Component placingComponent;
    private Breadboard placingBreadboard;
    private Color wireToolColor; // null = select mode; non-null = wire-placement mode
    private Pin wireStartPin;
    private boolean drawingWire;
    private double mouseX, mouseY;

    // Dragging state
    private boolean dragging;
    private boolean draggingBreadboard;
    private double dragOffsetX, dragOffsetY;
    private int draggingPivotIndex = -1;

    // Wire endpoint rerouting — 0 = start pin, 1 = end pin, -1 = not rerouting.
    private int reroutingEndpoint = -1;

    // Zoom and pan
    private double zoom = 1.0;
    private double panX = 0, panY = 0;
    private boolean panning;
    private double panStartX, panStartY;

    // Callbacks
    private Consumer<Component> onComponentSelected;
    private Consumer<String> onStatusMessage;
    private Consumer<String> onWarningMessage;
    private Runnable onToolFinished;
    /** Called just before any circuit-mutating action. Host uses it to snapshot for undo. */
    private Runnable onBeforeMutation;

    public CanvasView(Circuit circuit) {
        this.circuit = circuit;
        this.canvas = new Canvas(2000, 1500);
        this.gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        setStyle("-fx-background-color: " + Theme.BG_EDITOR + ";");

        // Bind canvas size to pane
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        widthProperty().addListener((obs, o, n) -> redraw());
        heightProperty().addListener((obs, o, n) -> redraw());

        setupMouseHandlers();
    }

    private void setupMouseHandlers() {
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        canvas.setOnMouseMoved(this::handleMouseMoved);
        canvas.setOnScroll(this::handleScroll);
    }

    private double toWorldX(double screenX) {
        return (screenX - panX) / zoom;
    }

    private double toWorldY(double screenY) {
        return (screenY - panY) / zoom;
    }

    private void handleMousePressed(MouseEvent e) {
        double wx = toWorldX(e.getX());
        double wy = toWorldY(e.getY());

        // Middle click or Ctrl+click for panning
        if (e.getButton() == MouseButton.MIDDLE || (e.getButton() == MouseButton.PRIMARY && e.isControlDown())) {
            panning = true;
            panStartX = e.getX() - panX;
            panStartY = e.getY() - panY;
            setCursor(Cursor.MOVE);
            return;
        }

        if (mode == AppMode.DRAW) {
            handleDrawPressed(e, wx, wy);
        } else if (mode == AppMode.SIMULATE) {
            handleSimulatePressed(e, wx, wy);
        }
    }

    private void handleDrawPressed(MouseEvent e, double wx, double wy) {

        // Right-click cancels in-progress placement, or adds/removes wire pivots.
        if (e.getButton() == MouseButton.SECONDARY) {
            // Cancel component / breadboard placement if one is active.
            if (placingComponent != null) {
                placingComponent = null;
                setCursor(Cursor.DEFAULT);
                statusMessage("Placement cancelled");
                redraw();
                fireToolFinished();
                return;
            }
            if (placingBreadboard != null) {
                placingBreadboard = null;
                setCursor(Cursor.DEFAULT);
                statusMessage("Placement cancelled");
                redraw();
                fireToolFinished();
                return;
            }
            if (selectedWire != null && selectedWire.isNear(wx, wy, 8)) {
                if (selectedWire.isLocked()) {
                    statusMessage("Wire is locked — press L to unlock");
                    return;
                }
                int pivotIdx = selectedWire.findWaypointIndex(wx, wy, 8);
                beforeMutation();
                if (pivotIdx >= 0) {
                    selectedWire.removeWaypoint(pivotIdx);
                    statusMessage("Pivot removed");
                } else {
                    selectedWire.addWaypointAt(wx, wy);
                    statusMessage("Pivot added");
                }
                redraw();
                return;
            }
            // Otherwise try to select a wire under the cursor and add/remove pivot
            Wire wireHit = findWireAt(wx, wy);
            if (wireHit != null) {
                selectedWire = wireHit;
                selectedComponent = null;
                if (onComponentSelected != null) onComponentSelected.accept(null);
                if (wireHit.isLocked()) {
                    statusMessage("Wire is locked — press L to unlock");
                    redraw();
                    return;
                }
                int pivotIdx = selectedWire.findWaypointIndex(wx, wy, 8);
                beforeMutation();
                if (pivotIdx >= 0) {
                    selectedWire.removeWaypoint(pivotIdx);
                    statusMessage("Pivot removed");
                } else {
                    selectedWire.addWaypointAt(wx, wy);
                    statusMessage("Pivot added");
                }
                redraw();
                return;
            }
            return;
        }

        // If placing a breadboard
        if (placingBreadboard != null) {
            double g = 20;
            double snapX = Math.round(wx / g) * g;
            double snapY = Math.round(wy / g) * g;
            beforeMutation();
            placingBreadboard.setPosition(snapX, snapY);
            circuit.addBreadboard(placingBreadboard);
            statusMessage("Breadboard placed");
            placingBreadboard = null;
            setCursor(Cursor.DEFAULT);
            redraw();
            fireToolFinished();
            return;
        }

        // If placing a component
        if (placingComponent != null) {
            // Refuse IC placement when the board has no free 7-column slot.
            if (placingComponent instanceof ICChip) {
                Breadboard bb = findNearestBreadboard(wx, wy);
                if (bb == null || findFreeIcSlot(bb, 0, (ICChip) placingComponent) < 0) {
                    warningMessage("No free space on breadboard for this IC");
                    redraw();
                    return;
                }
            }
            beforeMutation();
            double[] snap = snapForPlacement(placingComponent, wx, wy);
            placingComponent.setPosition(snap[0], snap[1]);
            insertPinsIntoHoles(placingComponent);
            circuit.addComponent(placingComponent);
            statusMessage("Placed " + placingComponent.getName());
            selectedComponent = placingComponent;
            if (onComponentSelected != null) onComponentSelected.accept(selectedComponent);
            placingComponent = null;
            setCursor(Cursor.DEFAULT);
            redraw();
            fireToolFinished();
            return;
        }

        // Wire tool: clicking pins/holes starts and finishes wires
        if (wireToolColor != null) {
            Pin hitPin = findPinAt(wx, wy, 12);
            if (hitPin != null) {
                if (drawingWire) {
                    finishWire(hitPin);
                } else {
                    startWire(hitPin);
                }
                return;
            }
            if (drawingWire) {
                drawingWire = false;
                wireStartPin = null;
                statusMessage("Wire cancelled");
                redraw();
                return;
            }
        }

        // Check if clicking an endpoint of the currently selected wire —
        // endpoint drag = reroute (reassign to a different pin). Locked wires
        // can't be rerouted.
        if (selectedWire != null && !selectedWire.isLocked()) {
            double sx = selectedWire.getStartPin().getX();
            double sy = selectedWire.getStartPin().getY();
            double ex = selectedWire.getEndPin().getX();
            double ey = selectedWire.getEndPin().getY();
            double r2 = 64; // 8 px pickup radius
            double dsx = sx - wx, dsy = sy - wy;
            double dex = ex - wx, dey = ey - wy;
            if (dsx * dsx + dsy * dsy <= r2) {
                beforeMutation();
                reroutingEndpoint = 0;
                setCursor(Cursor.CROSSHAIR);
                statusMessage("Drag to a pin or hole to reroute");
                return;
            }
            if (dex * dex + dey * dey <= r2) {
                beforeMutation();
                reroutingEndpoint = 1;
                setCursor(Cursor.CROSSHAIR);
                statusMessage("Drag to a pin or hole to reroute");
                return;
            }
        }

        // Check if clicking a pivot of the currently selected wire —
        // locked wires skip pivot drag.
        if (selectedWire != null && !selectedWire.isLocked()) {
            java.util.List<double[]> wps = selectedWire.getWaypoints();
            for (int i = 0; i < wps.size(); i++) {
                double[] wp = wps.get(i);
                double dx = wp[0] - wx, dy = wp[1] - wy;
                if (dx * dx + dy * dy <= 64) {
                    beforeMutation();
                    draggingPivotIndex = i;
                    setCursor(Cursor.MOVE);
                    return;
                }
            }
        }

        // Check if clicking a component (select/drag). Locked components
        // are selectable but can't be dragged.
        Component hit = findComponentAt(wx, wy);
        if (hit != null) {
            selectedComponent = hit;
            selectedWire = null;
            selectedBreadboard = null;
            if (!hit.isLocked()) {
                dragging = true;
                dragOffsetX = wx - hit.getX();
                dragOffsetY = wy - hit.getY();
                setCursor(Cursor.MOVE);
                // Snapshot before the drag begins — restoring undo returns the
                // component to its original position.
                beforeMutation();
            } else {
                statusMessage("Component is locked — press L to unlock");
            }
            if (onComponentSelected != null) onComponentSelected.accept(selectedComponent);
            redraw();
            return;
        }

        // Check if clicking a wire (before breadboard so wires on the board take priority)
        Wire wireHit = findWireAt(wx, wy);
        if (wireHit != null) {
            selectedWire = wireHit;
            selectedComponent = null;
            selectedBreadboard = null;
            if (onComponentSelected != null) onComponentSelected.accept(null);
            statusMessage("Wire selected — press Delete to remove");
            redraw();
            return;
        }

        // Check if clicking a breadboard border (select/drag)
        Breadboard bbHit = findBreadboardAt(wx, wy);
        if (bbHit != null) {
            selectedBreadboard = bbHit;
            selectedComponent = null;
            selectedWire = null;
            draggingBreadboard = true;
            dragOffsetX = wx - bbHit.getBoardX();
            dragOffsetY = wy - bbHit.getBoardY();
            if (onComponentSelected != null) onComponentSelected.accept(null);
            setCursor(Cursor.MOVE);
            statusMessage("Breadboard selected");
            beforeMutation();
            redraw();
            return;
        }

        // Empty space → start panning + deselect
        selectedComponent = null;
        selectedWire = null;
        selectedBreadboard = null;
        if (onComponentSelected != null) onComponentSelected.accept(null);
        panning = true;
        panStartX = e.getX() - panX;
        panStartY = e.getY() - panY;
        setCursor(Cursor.MOVE);
        redraw();
    }

    private void handleSimulatePressed(MouseEvent e, double wx, double wy) {
        Component hit = findComponentAt(wx, wy);
        if (hit instanceof Interactable) {
            if (hit instanceof DIPSwitch) {
                DIPSwitch dip = (DIPSwitch) hit;
                int idx = dip.getSwitchIndexAt(wx, wy);
                if (idx >= 0) {
                    dip.setLastToggledIndex(idx);
                }
            }
            ((Interactable) hit).onInteract();
            try {
                circuit.simulate();
                statusMessage("Simulated");
            } catch (OscillationException ex) {
                warningMessage("Oscillation detected!");
            } catch (Exception ex) {
                warningMessage("Simulation error: " + ex.getMessage());
            }
            selectedComponent = hit;
            if (onComponentSelected != null) onComponentSelected.accept(hit);
            redraw();
            return;
        }

        // Non-interactable component: select it so the properties panel
        // shows live pin states, but don't re-run simulation.
        if (hit != null) {
            selectedComponent = hit;
            if (onComponentSelected != null) onComponentSelected.accept(hit);
            statusMessage(hit.getName() + " selected");
            redraw();
            return;
        }

        // Clicked empty space — clear selection and start panning.
        selectedComponent = null;
        if (onComponentSelected != null) onComponentSelected.accept(null);
        panning = true;
        panStartX = e.getX() - panX;
        panStartY = e.getY() - panY;
        setCursor(Cursor.MOVE);
        redraw();
    }

    private void handleMouseDragged(MouseEvent e) {
        if (panning) {
            panX = e.getX() - panStartX;
            panY = e.getY() - panStartY;
            redraw();
            return;
        }

        double wx = toWorldX(e.getX());
        double wy = toWorldY(e.getY());

        if (mode == AppMode.DRAW && reroutingEndpoint >= 0 && selectedWire != null) {
            // Drag the endpoint — the ghost line is drawn in redraw() using mouseX/mouseY.
            mouseX = wx;
            mouseY = wy;
            redraw();
            return;
        }

        if (mode == AppMode.DRAW && draggingPivotIndex >= 0 && selectedWire != null) {
            double[] wp = selectedWire.getWaypoints().get(draggingPivotIndex);
            wp[0] = wx;
            wp[1] = wy;
            redraw();
            return;
        }

        if (mode == AppMode.DRAW && draggingBreadboard && selectedBreadboard != null) {
            double g = 20;
            double newX = Math.round((wx - dragOffsetX) / g) * g;
            double newY = Math.round((wy - dragOffsetY) / g) * g;
            double deltaX = newX - selectedBreadboard.getBoardX();
            double deltaY = newY - selectedBreadboard.getBoardY();
            if (deltaX == 0 && deltaY == 0) return;

            // Find components on this breadboard and move them along
            java.util.List<Component> onBoard = findComponentsOnBreadboard(selectedBreadboard);

            // Remove pins, move breadboard, move components, re-render, re-insert
            removeAllPinsFromBreadboard(selectedBreadboard);
            selectedBreadboard.setPosition(newX, newY);
            for (Component c : onBoard) {
                c.setPosition(c.getX() + deltaX, c.getY() + deltaY);
            }
            reinsertAllPinsIntoBreadboard(selectedBreadboard);
            redraw();
            return;
        }

        if (mode == AppMode.DRAW && dragging && selectedComponent != null) {
            removePinsFromHoles(selectedComponent);
            double targetCx = wx - dragOffsetX + selectedComponent.getWidth() / 2;
            double targetCy = wy - dragOffsetY + selectedComponent.getHeight() / 2;
            if (selectedComponent instanceof ICChip) {
                Breadboard bb = findNearestBreadboard(targetCx, targetCy);
                if (bb != null) {
                    double halfW = selectedComponent.getWidth() / 2;
                    double minCx = bb.getBoardX() + halfW;
                    double maxCx = bb.getBoardX() + bb.getBoardWidth() - halfW;
                    targetCx = Math.max(minCx, Math.min(maxCx, targetCx));
                    targetCy = bb.getBoardY() + bb.getBoardHeight() / 2;
                }
            }
            double[] snap = snapForPlacement(selectedComponent, targetCx, targetCy);
            selectedComponent.setPosition(snap[0], snap[1]);
            insertPinsIntoHoles(selectedComponent);
            if (onComponentSelected != null) onComponentSelected.accept(selectedComponent);
            redraw();
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        if (panning) {
            panning = false;
            setCursor(Cursor.DEFAULT);
            return;
        }

        if (dragging) {
            dragging = false;
            setCursor(Cursor.DEFAULT);
        }

        if (draggingBreadboard) {
            draggingBreadboard = false;
            setCursor(Cursor.DEFAULT);
        }

        if (draggingPivotIndex >= 0) {
            draggingPivotIndex = -1;
            setCursor(Cursor.DEFAULT);
        }

        if (reroutingEndpoint >= 0 && selectedWire != null) {
            double wx = toWorldX(e.getX());
            double wy = toWorldY(e.getY());
            Pin newPin = findPinAt(wx, wy, 12);
            Pin otherEnd = reroutingEndpoint == 0 ? selectedWire.getEndPin() : selectedWire.getStartPin();
            Pin currentEnd = reroutingEndpoint == 0 ? selectedWire.getStartPin() : selectedWire.getEndPin();
            if (newPin == null) {
                warningMessage("Reroute cancelled — drop onto a pin or hole");
            } else if (newPin == currentEnd) {
                // No-op: released on the same pin we started from.
            } else if (newPin == otherEnd) {
                warningMessage("Can't connect a wire to itself");
            } else if (newPin.getOwner() == otherEnd.getOwner()) {
                warningMessage("Can't wire pins on the same component");
            } else {
                if (reroutingEndpoint == 0) {
                    selectedWire.setStartPin(newPin);
                } else {
                    selectedWire.setEndPin(newPin);
                }
                statusMessage("Wire rerouted");
            }
            reroutingEndpoint = -1;
            setCursor(Cursor.DEFAULT);
            redraw();
            return;
        }

        // Handle PushButton release in simulate mode
        if (mode == AppMode.SIMULATE) {
            double wx = toWorldX(e.getX());
            double wy = toWorldY(e.getY());
            Component hit = findComponentAt(wx, wy);
            if (hit instanceof PushButton) {
                ((PushButton) hit).onRelease();
                try {
                    circuit.simulate();
                } catch (OscillationException ex) {
                    warningMessage("Oscillation detected!");
                } catch (Exception ex) {
                    warningMessage("Simulation error: " + ex.getMessage());
                }
                redraw();
            }
        }
    }

    private void handleMouseMoved(MouseEvent e) {
        mouseX = toWorldX(e.getX());
        mouseY = toWorldY(e.getY());

        if (placingBreadboard != null) {
            double g = 20;
            placingBreadboard.setPosition(Math.round(mouseX / g) * g, Math.round(mouseY / g) * g);
            redraw();
        } else if (placingComponent != null) {
            double[] snap = snapForPlacement(placingComponent, mouseX, mouseY);
            placingComponent.setPosition(snap[0], snap[1]);
            redraw();
        } else if (drawingWire) {
            redraw();
        }
    }

    private void handleScroll(ScrollEvent e) {
        double zoomFactor = e.getDeltaY() > 0 ? 1.1 : 0.9;
        double oldZoom = zoom;
        zoom = Math.max(0.3, Math.min(3.0, zoom * zoomFactor));

        // Zoom towards mouse position
        double mouseScreenX = e.getX();
        double mouseScreenY = e.getY();
        panX = mouseScreenX - (mouseScreenX - panX) * (zoom / oldZoom);
        panY = mouseScreenY - (mouseScreenY - panY) * (zoom / oldZoom);

        redraw();
    }

    private void startWire(Pin pin) {
        wireStartPin = pin;
        drawingWire = true;
        statusMessage("Wire started from " + pin.getOwner().getName() + " pin " + pin.getLabel());
    }

    private void finishWire(Pin endPin) {
        try {
            if (wireStartPin == endPin) {
                throw new InvalidConnectionException("Can't connect pin to itself");
            }
            if (wireStartPin.getOwner() == endPin.getOwner()) {
                throw new InvalidConnectionException("Can't wire pins on the same component");
            }

            beforeMutation();
            Wire wire = new Wire(wireStartPin, endPin);
            if (wireToolColor != null) wire.setColor(wireToolColor);
            circuit.addWire(wire);
            statusMessage("Wire connected: " + wireStartPin.getOwner().getName() + " → " + endPin.getOwner().getName());
        } catch (InvalidConnectionException ex) {
            warningMessage(ex.getMessage());
        } finally {
            drawingWire = false;
            wireStartPin = null;
            redraw();
        }
    }

    public void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // Background — IntelliJ editor color
        gc.setFill(Color.web(Theme.BG_EDITOR));
        gc.fillRect(0, 0, w, h);

        // Draw grid pattern
        gc.save();
        gc.translate(panX, panY);
        gc.scale(zoom, zoom);

        drawGrid();

        // 1. Draw all breadboards
        for (Breadboard bb : circuit.getBreadboards()) {
            bb.render(gc);

            // Selection highlight for breadboard
            if (bb == selectedBreadboard) {
                gc.setStroke(Color.rgb(0, 120, 212, 0.8));
                gc.setLineWidth(2);
                gc.setLineDashes(6, 4);
                gc.strokeRoundRect(bb.getBoardX() - 23, bb.getBoardY() - 23,
                        bb.getBoardWidth() + 6, bb.getBoardHeight() + 6, 12, 12);
                gc.setLineDashes();
            }
        }

        // 2. Draw wires
        boolean simMode = mode == AppMode.SIMULATE;
        for (Wire wire : circuit.getWires()) {
            // While rerouting, draw the selected wire with the dragged endpoint
            // snapped to the mouse so the user sees a live preview.
            boolean isRerouting = (wire == selectedWire) && reroutingEndpoint >= 0;

            if (wire == selectedWire) {
                // Glow underneath the wire, following waypoints
                gc.setStroke(Color.rgb(0, 180, 255, 0.7));
                gc.setLineWidth(8);
                gc.beginPath();
                double sx = wire.getStartPin().getX();
                double sy = wire.getStartPin().getY();
                double ex = wire.getEndPin().getX();
                double ey = wire.getEndPin().getY();
                if (isRerouting && reroutingEndpoint == 0) { sx = mouseX; sy = mouseY; }
                if (isRerouting && reroutingEndpoint == 1) { ex = mouseX; ey = mouseY; }
                gc.moveTo(sx, sy);
                for (double[] wp : wire.getWaypoints()) {
                    gc.lineTo(wp[0], wp[1]);
                }
                gc.lineTo(ex, ey);
                gc.stroke();
            }

            if (isRerouting) {
                // Skip the wire's own render — it'd draw from the original pins.
                // Draw a dashed preview along the same path instead.
                gc.setStroke(wire.getColor());
                gc.setLineWidth(2);
                gc.setLineDashes(6, 5);
                double sx = wire.getStartPin().getX();
                double sy = wire.getStartPin().getY();
                double ex = wire.getEndPin().getX();
                double ey = wire.getEndPin().getY();
                if (reroutingEndpoint == 0) { sx = mouseX; sy = mouseY; }
                if (reroutingEndpoint == 1) { ex = mouseX; ey = mouseY; }
                gc.beginPath();
                gc.moveTo(sx, sy);
                for (double[] wp : wire.getWaypoints()) {
                    gc.lineTo(wp[0], wp[1]);
                }
                gc.lineTo(ex, ey);
                gc.stroke();
                gc.setLineDashes();
            } else {
                wire.render(gc, simMode);
            }

            if (wire == selectedWire) {
                // Pivot handles
                gc.setFill(Color.rgb(0, 180, 255));
                for (double[] wp : wire.getWaypoints()) {
                    gc.fillOval(wp[0] - 4, wp[1] - 4, 8, 8);
                }
                // Endpoint handles — bigger, white-ringed, so it's obvious they
                // can be dragged to reroute the wire.
                double sx = wire.getStartPin().getX();
                double sy = wire.getStartPin().getY();
                double ex = wire.getEndPin().getX();
                double ey = wire.getEndPin().getY();
                if (isRerouting && reroutingEndpoint == 0) { sx = mouseX; sy = mouseY; }
                if (isRerouting && reroutingEndpoint == 1) { ex = mouseX; ey = mouseY; }
                gc.setFill(Color.rgb(0, 180, 255));
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.5);
                gc.fillOval(sx - 6, sy - 6, 12, 12);
                gc.strokeOval(sx - 6, sy - 6, 12, 12);
                gc.fillOval(ex - 6, ey - 6, 12, 12);
                gc.strokeOval(ex - 6, ey - 6, 12, 12);
            }
        }

        // 3. Draw components
        for (Component c : circuit.getComponents()) {
            c.render(gc);

            // Selection highlight
            if (c == selectedComponent) {
                gc.setStroke(Color.rgb(0, 120, 212, 0.8));
                gc.setLineWidth(2);
                gc.setLineDashes(6, 4);
                gc.strokeRect(c.getX() - 3, c.getY() - 3, c.getWidth() + 6, c.getHeight() + 6);
                gc.setLineDashes();
            }

            // Locked badge: a tiny padlock in the top-right corner of the component.
            if (c.isLocked()) {
                drawLockBadge(c.getX() + c.getWidth() - 10, c.getY() - 2);
            }
        }

        // 4. Ghost component being placed
        if (placingComponent != null) {
            gc.setGlobalAlpha(0.6);
            placingComponent.render(gc);
            gc.setGlobalAlpha(1.0);
        }

        // 4b. Ghost breadboard being placed
        if (placingBreadboard != null) {
            gc.setGlobalAlpha(0.5);
            placingBreadboard.render(gc);
            gc.setGlobalAlpha(1.0);
        }

        // 5. Wire being drawn
        if (drawingWire && wireStartPin != null) {
            gc.setStroke(wireToolColor != null ? wireToolColor : Color.rgb(200, 200, 200, 0.7));
            gc.setLineWidth(2);
            gc.setLineDashes(5, 5);
            gc.strokeLine(wireStartPin.getX(), wireStartPin.getY(), mouseX, mouseY);
            gc.setLineDashes();
        }

        gc.restore();
    }

    /** Small padlock badge drawn at (x,y) as the top-left of a 10x10 box. */
    private void drawLockBadge(double x, double y) {
        // Shackle (arc)
        gc.setStroke(Color.rgb(255, 210, 80));
        gc.setLineWidth(1.2);
        gc.strokeArc(x + 1.5, y, 7, 6, 0, 180, javafx.scene.shape.ArcType.OPEN);
        // Body
        gc.setFill(Color.rgb(255, 210, 80));
        gc.fillRoundRect(x, y + 4, 10, 6, 2, 2);
        // Keyhole
        gc.setFill(Color.rgb(60, 45, 0));
        gc.fillOval(x + 4, y + 6, 2, 2);
    }

    private void drawGrid() {
        gc.setStroke(Color.rgb(55, 55, 55));
        gc.setLineWidth(0.5);
        double gridSize = 20;
        double startX = -panX / zoom;
        double startY = -panY / zoom;
        double endX = startX + canvas.getWidth() / zoom;
        double endY = startY + canvas.getHeight() / zoom;

        startX = Math.floor(startX / gridSize) * gridSize;
        startY = Math.floor(startY / gridSize) * gridSize;

        for (double x = startX; x <= endX; x += gridSize) {
            gc.strokeLine(x, startY, x, endY);
        }
        for (double y = startY; y <= endY; y += gridSize) {
            gc.strokeLine(startX, y, endX, y);
        }
    }

    private Component findComponentAt(double x, double y) {
        // Iterate in reverse for top-most first
        for (int i = circuit.getComponents().size() - 1; i >= 0; i--) {
            Component c = circuit.getComponents().get(i);
            if (c.containsPoint(x, y)) {
                return c;
            }
        }
        return null;
    }

    private Breadboard findBreadboardAt(double x, double y) {
        // Only select breadboard when clicking on the border/outskirts, not interior
        java.util.List<Breadboard> bbs = circuit.getBreadboards();
        for (int i = bbs.size() - 1; i >= 0; i--) {
            if (bbs.get(i).containsPointOnBorder(x, y)) {
                return bbs.get(i);
            }
        }
        return null;
    }

    private Pin findPinAt(double x, double y, double radius) {
        // 1. Component pins take priority
        for (Component c : circuit.getComponents()) {
            for (Pin p : c.getPins()) {
                double dx = p.getX() - x;
                double dy = p.getY() - y;
                if (dx * dx + dy * dy <= radius * radius) {
                    return p;
                }
            }
        }
        // 2. Bare breadboard holes — search all breadboards
        double r2 = radius * radius;
        for (Breadboard bb : circuit.getBreadboards()) {
            for (int col = 0; col < bb.getRows(); col++) {
                for (int letter = 0; letter < 10; letter++) {
                    ContactPoint cp = bb.getHole(col, letter);
                    if (cp == null) continue;
                    double dx = cp.getCanvasX() - x;
                    double dy = cp.getCanvasY() - y;
                    if (dx * dx + dy * dy <= r2) {
                        return cp.getOrCreateHolePin();
                    }
                }
            }
            // 3. Power rail holes
            PowerRail[] rails = { bb.getTopPositive(), bb.getTopNegative(), bb.getBottomPositive(), bb.getBottomNegative() };
            for (PowerRail rail : rails) {
                for (ContactPoint cp : rail.getPoints()) {
                    double dx = cp.getCanvasX() - x;
                    double dy = cp.getCanvasY() - y;
                    if (dx * dx + dy * dy <= r2) {
                        return cp.getOrCreateHolePin();
                    }
                }
            }
        }
        return null;
    }

    /** Inserts a component's pins into any breadboard holes (or rail holes) they sit on. */
    private void insertPinsIntoHoles(Component c) {
        if (c instanceof ICChip) {
            attachIcWires((ICChip) c);
            return;
        }
        for (Pin p : c.getPins()) {
            ContactPoint cp = findHoleAt(p.getX(), p.getY());
            if (cp != null) cp.insertPin(p);
        }
    }

    private void removePinsFromHoles(Component c) {
        if (c instanceof ICChip) {
            detachIcWires((ICChip) c);
            return;
        }
        for (Pin p : c.getPins()) {
            ContactPoint cp = findHoleAt(p.getX(), p.getY());
            if (cp != null && cp.getOccupant() == p) cp.removePin();
        }
    }

    /**
     * Wires an IC into its breadboard. Instead of inserting the IC's pins directly
     * into ContactPoints by proximity, this creates 14 explicit Wire objects — one
     * per pin — that run through the IC slot to the corresponding breadboard hole:
     *   pins 1..7  → row 'f' (letter 5), columns startCol..startCol+6
     *   pins 14..8 → row 'e' (letter 4), columns startCol..startCol+6
     * Uses the IC's current X and the breadboard's hole grid to pick the column
     * deterministically — no distance-based matching, no floating-point surprises.
     */
    private void attachIcWires(ICChip ic) {
        // Skip if this IC already has slot wires (e.g. after undo/redo deserialization)
        for (Wire w : circuit.getWires()) {
            Pin s = w.getStartPin();
            Pin e = w.getEndPin();
            if ((s.getOwner() == ic && e instanceof HolePin) ||
                (e.getOwner() == ic && s instanceof HolePin)) return;
        }

        Breadboard bb = findNearestBreadboard(ic.getX() + ic.getWidth() / 2,
                                              ic.getY() + ic.getHeight() / 2);
        if (bb == null) return;

        // snappedX = bb.getHoleX(startCol) - 10, so startCol = (ic.getX() + 10 - boardX) / HOLE_SPACING
        int startCol = (int) Math.round((ic.getX() + 10 - bb.getBoardX()) / Breadboard.HOLE_SPACING);
        if (startCol < 0 || startCol + 6 >= bb.getRows()) return;

        // Bottom row: pins 1..7 land on row 'f' (letter index 5)
        for (int i = 0; i < 7; i++) {
            Pin icPin = ic.getPin(String.valueOf(i + 1));
            ContactPoint cp = bb.getHole(startCol + i, 5);
            if (icPin == null || cp == null) continue;
            HolePin hp = cp.getOrCreateHolePin();
            hp.setPosition(cp.getCanvasX(), cp.getCanvasY());
            circuit.addWire(new Wire(icPin, hp));
        }
        // Top row: pin 14 at startCol, pin 13 at startCol+1, ..., pin 8 at startCol+6, row 'e' (letter 4)
        for (int i = 0; i < 7; i++) {
            Pin icPin = ic.getPin(String.valueOf(14 - i));
            ContactPoint cp = bb.getHole(startCol + i, 4);
            if (icPin == null || cp == null) continue;
            HolePin hp = cp.getOrCreateHolePin();
            hp.setPosition(cp.getCanvasX(), cp.getCanvasY());
            circuit.addWire(new Wire(icPin, hp));
        }
    }

    /** Removes every wire that links this IC's pins to a breadboard hole. */
    private void detachIcWires(ICChip ic) {
        java.util.List<Wire> toRemove = new java.util.ArrayList<>();
        for (Wire w : circuit.getWires()) {
            Pin s = w.getStartPin();
            Pin e = w.getEndPin();
            boolean startIsIc = s.getOwner() == ic;
            boolean endIsIc = e.getOwner() == ic;
            boolean startIsHole = s instanceof HolePin;
            boolean endIsHole = e instanceof HolePin;
            if ((startIsIc && endIsHole) || (endIsIc && startIsHole)) {
                toRemove.add(w);
            }
        }
        for (Wire w : toRemove) circuit.removeWire(w);
    }

    private ContactPoint findHoleAt(double x, double y) {
        double tol = 4; // pin must land within 4 px of a hole center
        double t2 = tol * tol;
        for (Breadboard bb : circuit.getBreadboards()) {
            // Main grid
            for (int col = 0; col < bb.getRows(); col++) {
                for (int letter = 0; letter < 10; letter++) {
                    ContactPoint cp = bb.getHole(col, letter);
                    if (cp == null) continue;
                    double dx = cp.getCanvasX() - x;
                    double dy = cp.getCanvasY() - y;
                    if (dx * dx + dy * dy <= t2) return cp;
                }
            }
            // Power rails
            PowerRail[] rails = { bb.getTopPositive(), bb.getTopNegative(), bb.getBottomPositive(), bb.getBottomNegative() };
            for (PowerRail rail : rails) {
                for (ContactPoint cp : rail.getPoints()) {
                    double dx = cp.getCanvasX() - x;
                    double dy = cp.getCanvasY() - y;
                    if (dx * dx + dy * dy <= t2) return cp;
                }
            }
        }
        return null;
    }

    /**
     * Finds all components attached to this breadboard — either via proximity-inserted
     * pins (switches, LEDs, etc.) or via IC-style HolePin wires (chips use slot wires
     * instead of direct insertion, so they're not caught by {@code cp.getOccupant()}).
     */
    private java.util.List<Component> findComponentsOnBreadboard(Breadboard bb) {
        java.util.Set<Component> result = new java.util.LinkedHashSet<>();
        // 1. Directly-inserted pins
        for (ContactPoint cp : bb.getAllContactPoints()) {
            if (cp.getOccupant() != null && cp.getOccupant().getOwner() != null) {
                result.add(cp.getOccupant().getOwner());
            }
        }
        // 2. ICs wired to this board via HolePin slot wires
        java.util.Set<ContactPoint> bbPoints = new java.util.HashSet<>(bb.getAllContactPoints());
        for (Wire w : circuit.getWires()) {
            Pin s = w.getStartPin();
            Pin e = w.getEndPin();
            Component icSide = null;
            if (s instanceof HolePin && bbPoints.contains(((HolePin) s).getContact())) {
                if (e.getOwner() instanceof ICChip) icSide = e.getOwner();
            } else if (e instanceof HolePin && bbPoints.contains(((HolePin) e).getContact())) {
                if (s.getOwner() instanceof ICChip) icSide = s.getOwner();
            }
            if (icSide != null) result.add(icSide);
        }
        return new java.util.ArrayList<>(result);
    }

    /** Removes all component pins that are currently inserted into a specific breadboard's holes. */
    private void removeAllPinsFromBreadboard(Breadboard bb) {
        for (ContactPoint cp : bb.getAllContactPoints()) {
            if (cp.getOccupant() != null) {
                cp.removePin();
            }
        }
    }

    /** Re-inserts component pins into a breadboard's holes after it has been moved. */
    private void reinsertAllPinsIntoBreadboard(Breadboard bb) {
        for (Component c : circuit.getComponents()) {
            // ICs are wired in, not proximity-inserted — rebuild their slot wires
            if (c instanceof ICChip) {
                detachIcWires((ICChip) c);
                attachIcWires((ICChip) c);
                continue;
            }
            for (Pin p : c.getPins()) {
                // Check main grid
                for (int col = 0; col < bb.getRows(); col++) {
                    for (int letter = 0; letter < 10; letter++) {
                        ContactPoint cp = bb.getHole(col, letter);
                        if (cp == null) continue;
                        double dx = cp.getCanvasX() - p.getX();
                        double dy = cp.getCanvasY() - p.getY();
                        if (dx * dx + dy * dy <= 16) cp.insertPin(p);
                    }
                }
                // Check power rails
                PowerRail[] rails = { bb.getTopPositive(), bb.getTopNegative(), bb.getBottomPositive(), bb.getBottomNegative() };
                for (PowerRail rail : rails) {
                    for (ContactPoint cp : rail.getPoints()) {
                        double dx = cp.getCanvasX() - p.getX();
                        double dy = cp.getCanvasY() - p.getY();
                        if (dx * dx + dy * dy <= 16) cp.insertPin(p);
                    }
                }
            }
        }
    }

    /** Snap a component to the nearest breadboard. ICs auto-straddle the center gap. */
    private double[] snapForPlacement(Component c, double wx, double wy) {
        if (c instanceof ICChip) {
            Breadboard bb = findNearestBreadboard(wx, wy);
            double pinSpacing = 20;
            double targetPinX = wx + c.getWidth() / 2 - 3 * pinSpacing;
            int nearestCol = (int) Math.round((targetPinX - bb.getHoleX(0)) / pinSpacing);
            nearestCol = Math.max(0, Math.min(bb.getRows() - 7, nearestCol));
            // Slide to the nearest free slot so ICs can't overlap. Falls back to the
            // preferred column if no free slot exists — the commit path will refuse.
            int freeCol = findFreeIcSlot(bb, nearestCol, (ICChip) c);
            int useCol = freeCol >= 0 ? freeCol : nearestCol;
            double snappedX = bb.getHoleX(useCol) - 10;
            double snappedY = bb.getHoleY(4);
            return new double[]{snappedX, snappedY};
        }
        double g = 20;
        return new double[]{Math.round(wx / g) * g, Math.round(wy / g) * g};
    }

    /** True if no other IC on {@code bb} occupies the 7 columns starting at {@code startCol}. */
    private boolean isIcSlotFree(Breadboard bb, int startCol, ICChip excludeIc) {
        for (Component c : circuit.getComponents()) {
            if (!(c instanceof ICChip) || c == excludeIc) continue;
            ICChip other = (ICChip) c;
            Breadboard otherBb = findNearestBreadboard(other.getX() + other.getWidth() / 2,
                                                       other.getY() + other.getHeight() / 2);
            if (otherBb != bb) continue;
            int otherStart = (int) Math.round((other.getX() + 10 - bb.getBoardX()) / Breadboard.HOLE_SPACING);
            // Two 7-column ranges overlap when start+6 >= otherStart && otherStart+6 >= start.
            if (startCol + 6 >= otherStart && otherStart + 6 >= startCol) {
                return false;
            }
        }
        return true;
    }

    /** Finds the free IC slot closest to {@code preferredCol}, or -1 if the board has no room. */
    private int findFreeIcSlot(Breadboard bb, int preferredCol, ICChip excludeIc) {
        int max = bb.getRows() - 7;
        if (max < 0) return -1;
        int p = Math.max(0, Math.min(max, preferredCol));
        if (isIcSlotFree(bb, p, excludeIc)) return p;
        for (int d = 1; d <= max; d++) {
            if (p - d >= 0 && isIcSlotFree(bb, p - d, excludeIc)) return p - d;
            if (p + d <= max && isIcSlotFree(bb, p + d, excludeIc)) return p + d;
        }
        return -1;
    }

    /** Finds the breadboard closest to the given world coordinate. */
    private Breadboard findNearestBreadboard(double wx, double wy) {
        Breadboard nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (Breadboard bb : circuit.getBreadboards()) {
            double cx = bb.getBoardX() + bb.getBoardWidth() / 2;
            double cy = bb.getBoardY() + bb.getBoardHeight() / 2;
            double dist = (cx - wx) * (cx - wx) + (cy - wy) * (cy - wy);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = bb;
            }
        }
        return nearest;
    }

    private Wire findWireAt(double x, double y) {
        for (Wire w : circuit.getWires()) {
            if (w.isNear(x, y, 5)) {
                return w;
            }
        }
        return null;
    }

    public void startPlacement(Component c) {
        this.placingComponent = c;
        this.placingBreadboard = null;
        this.wireToolColor = null;
        this.drawingWire = false;
        this.wireStartPin = null;
        c.setPosition(mouseX, mouseY);
        setCursor(Cursor.CROSSHAIR);
        statusMessage("Click to place " + c.getName());
    }

    public void startBreadboardPlacement() {
        Breadboard bb = new Breadboard(30);
        bb.setPosition(mouseX, mouseY);
        this.placingBreadboard = bb;
        this.placingComponent = null;
        this.wireToolColor = null;
        this.drawingWire = false;
        this.wireStartPin = null;
        setCursor(Cursor.CROSSHAIR);
        statusMessage("Click to place breadboard");
    }

    public void setWireToolColor(Color color) {
        this.wireToolColor = color;
        this.placingComponent = null;
        this.drawingWire = false;
        this.wireStartPin = null;
        if (color != null) {
            setCursor(Cursor.CROSSHAIR);
            statusMessage("Wire tool: click two pins or holes to connect");
        } else {
            setCursor(Cursor.DEFAULT);
            statusMessage("Select mode");
        }
        redraw();
    }

    public void setMode(AppMode mode) {
        this.mode = mode;
        if (mode == AppMode.SIMULATE) {
            placingComponent = null;
            placingBreadboard = null;
            drawingWire = false;
            wireStartPin = null;
            try {
                circuit.simulate();
            } catch (OscillationException ex) {
                warningMessage("Oscillation detected!");
            } catch (Exception ex) {
                warningMessage("Simulation error: " + ex.getMessage());
            }
        } else {
            // Reset all states when leaving simulate mode
            circuit.resetStates();
        }
        redraw();
    }

    public void setCircuit(Circuit circuit) {
        this.circuit = circuit;
        selectedComponent = null;
        selectedWire = null;
        selectedBreadboard = null;
        placingComponent = null;
        placingBreadboard = null;
        drawingWire = false;
        wireStartPin = null;
        reroutingEndpoint = -1;
        // Force a render so contact points get canvas positions, then bind pins
        redraw();
        for (Component c : circuit.getComponents()) {
            insertPinsIntoHoles(c);
        }
        redraw();
    }

    public Component getSelectedComponent() {
        return selectedComponent;
    }

    public Wire getSelectedWire() {
        return selectedWire;
    }

    /** Toggles the locked state of whatever is currently selected. Used by the L keybind. */
    public boolean toggleLockOnSelection() {
        if (selectedComponent != null) {
            beforeMutation();
            boolean now = !selectedComponent.isLocked();
            selectedComponent.setLocked(now);
            statusMessage(selectedComponent.getName() + (now ? " locked" : " unlocked"));
            if (onComponentSelected != null) onComponentSelected.accept(selectedComponent);
            redraw();
            return true;
        }
        if (selectedWire != null) {
            beforeMutation();
            boolean now = !selectedWire.isLocked();
            selectedWire.setLocked(now);
            statusMessage("Wire " + (now ? "locked" : "unlocked"));
            // Force a properties-panel refresh; wires don't flow through onComponentSelected
            // so we re-notify it via the current selection (may be null).
            if (onComponentSelected != null) onComponentSelected.accept(selectedComponent);
            redraw();
            return true;
        }
        return false;
    }

    /** Directly sets the locked state on the current selection — used by the properties-panel toggle. */
    public void setLockOnSelection(boolean locked) {
        if (selectedComponent != null && selectedComponent.isLocked() != locked) {
            beforeMutation();
            selectedComponent.setLocked(locked);
            redraw();
        } else if (selectedWire != null && selectedWire.isLocked() != locked) {
            beforeMutation();
            selectedWire.setLocked(locked);
            redraw();
        }
    }

    /**
     * Changes the pin side on the selected input component. Rips its pins
     * out of any breadboard holes first and reinserts them afterward so
     * column-net membership follows the new positions.
     */
    public void setPinSideOnSelection(PinSide side) {
        if (selectedComponent instanceof InputComponent) {
            InputComponent ic = (InputComponent) selectedComponent;
            if (ic.getPinSide() == side) return;
            beforeMutation();
            removePinsFromHoles(ic);
            ic.setPinSide(side);
            insertPinsIntoHoles(ic);
            redraw();
        } else if (selectedComponent instanceof com.logiclab.model.output.LightBar) {
            com.logiclab.model.output.LightBar lb = (com.logiclab.model.output.LightBar) selectedComponent;
            if (lb.getPinSide() == side) return;
            beforeMutation();
            removePinsFromHoles(lb);
            lb.setPinSide(side);
            insertPinsIntoHoles(lb);
            redraw();
        }
    }

    public void clearWireSelection() {
        selectedWire = null;
    }

    public void setOnComponentSelected(Consumer<Component> handler) {
        this.onComponentSelected = handler;
    }

    public void setOnStatusMessage(Consumer<String> handler) {
        this.onStatusMessage = handler;
    }

    public void setOnWarningMessage(Consumer<String> handler) {
        this.onWarningMessage = handler;
    }

    public void setOnToolFinished(Runnable handler) {
        this.onToolFinished = handler;
    }

    public void setOnBeforeMutation(Runnable handler) {
        this.onBeforeMutation = handler;
    }

    /** Called at every mutation site in CanvasView before state changes. */
    private void beforeMutation() {
        if (onBeforeMutation != null) onBeforeMutation.run();
    }

    private void fireToolFinished() {
        if (onToolFinished != null) onToolFinished.run();
    }

    private void statusMessage(String msg) {
        if (onStatusMessage != null) onStatusMessage.accept(msg);
    }

    private void warningMessage(String msg) {
        if (onWarningMessage != null) onWarningMessage.accept(msg);
    }
}
