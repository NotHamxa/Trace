package com.trace.model;

import com.trace.interfaces.Renderable;
import com.trace.interfaces.Simulatable;
import javafx.scene.canvas.GraphicsContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Component implements Serializable, Renderable, Simulatable {
    private String id;
    private String name;
    private double x, y;
    private double width, height;
    private List<Pin> pins;
    private boolean placed;
    private boolean locked;
    private String displayLabel;

    private static int idCounter = 0;

    public Component(String name, double width, double height) {
        this.id = name.replaceAll("\\s+", "_") + "_" + (++idCounter);
        this.name = name;
        this.width = width;
        this.height = height;
        this.pins = new ArrayList<>();
        this.placed = false;
    }

    public abstract void simulate();
    public abstract void render(GraphicsContext gc);
    public abstract Component clone();

    protected void addPin(Pin pin) {
        pins.add(pin);
    }

    public List<Pin> getPins() {
        return pins;
    }

    public Pin getPin(String label) {
        for (Pin p : pins) {
            if (p.getLabel().equals(label)) {
                return p;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

    /** Used by the JSON loader to restore saved component ids. */
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        updatePinPositions();
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    protected void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public boolean isPlaced() {
        return placed;
    }

    public void setPlaced(boolean placed) {
        this.placed = placed;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /** Optional human-readable label used by the test table (e.g. "A", "Cin", "SUM"). */
    public String getDisplayLabel() {
        return displayLabel;
    }

    public void setDisplayLabel(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public boolean containsPoint(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    protected void updatePinPositions() {
        // Subclasses override to position pins relative to component
    }

    public void restorePinOwnership() {
        for (Pin p : pins) {
            p.setOwner(this);
        }
    }
}
