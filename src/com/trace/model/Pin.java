package com.trace.model;

import com.trace.interfaces.Renderable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.io.Serializable;

public class Pin implements Serializable, Renderable {
    private String label;
    private PinType type;
    private LogicState state;
    private transient Component owner;
    private double x, y;

    public Pin(String label, PinType type, Component owner) {
        this.label = label;
        this.type = type;
        this.owner = owner;
        this.state = LogicState.FLOATING;
    }

    public String getLabel() {
        return label;
    }

    public PinType getType() {
        return type;
    }

    public LogicState getState() {
        return state;
    }

    public void setState(LogicState state) {
        this.state = state;
    }

    public Component getOwner() {
        return owner;
    }

    public void setOwner(Component owner) {
        this.owner = owner;
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
    }

    @Override
    public void render(GraphicsContext gc) {
        double radius = 3;
        switch (state) {
            case HIGH:
                gc.setFill(Color.LIMEGREEN);
                break;
            case LOW:
                gc.setFill(Color.DARKBLUE);
                break;
            case FLOATING:
                gc.setFill(Color.GRAY);
                break;
        }
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5);
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
    }
}
