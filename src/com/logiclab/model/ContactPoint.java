package com.logiclab.model;

import java.io.Serializable;

public class ContactPoint implements Serializable {
    private int row, col;
    private Net net;
    private Pin occupant;
    private double canvasX, canvasY;
    private HolePin holePin;

    public ContactPoint(int row, int col, Net net) {
        this.row = row;
        this.col = col;
        this.net = net;
        net.addPoint(this);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isOccupied() {
        return occupant != null;
    }

    public Pin getOccupant() {
        return occupant;
    }

    public void insertPin(Pin pin) {
        this.occupant = pin;
    }

    public void removePin() {
        this.occupant = null;
    }

    public Net getNet() {
        return net;
    }

    public double getCanvasX() {
        return canvasX;
    }

    public double getCanvasY() {
        return canvasY;
    }

    public void setCanvasPosition(double x, double y) {
        this.canvasX = x;
        this.canvasY = y;
    }

    /** Returns the existing HolePin if one was created, or null. Does NOT create one. */
    public HolePin getHolePinIfExists() {
        return holePin;
    }

    /** Lazily creates a Pin representing this bare breadboard hole, for wires. */
    public HolePin getOrCreateHolePin() {
        if (holePin == null) {
            BreadboardHoleAnchor anchor = new BreadboardHoleAnchor(row, col);
            holePin = new HolePin(this, anchor);
        }
        return holePin;
    }
}
