package com.trace.model.passive;

import com.trace.model.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class Resistor extends PassiveComponent {
    private int resistance;

    public Resistor() {
        this(220);
    }

    public Resistor(int resistance) {
        super("Resistor", 40, 12);
        this.resistance = resistance;
        addPin(new Pin("1", PinType.INPUT, this));
        addPin(new Pin("2", PinType.OUTPUT, this));
    }

    @Override
    public void simulate() {
        if (getPin("1").getState() != LogicState.FLOATING) {
            getPin("2").setState(getPin("1").getState());
        } else if (getPin("2").getState() != LogicState.FLOATING) {
            getPin("1").setState(getPin("2").getState());
        }
    }

    @Override
    protected void updatePinPositions() {
        getPin("1").setPosition(getX(), getY() + getHeight() / 2);
        getPin("2").setPosition(getX() + getWidth(), getY() + getHeight() / 2);
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        double cy = y + h / 2;

        // Lead wires — light silver so they read on dark editor bg
        gc.setStroke(Color.rgb(200, 203, 208));
        gc.setLineWidth(1.5);
        gc.strokeLine(x, cy, x + 8, cy);
        gc.strokeLine(x + w - 8, cy, x + w, cy);

        // Resistor body — slightly deeper tan, rounded
        gc.setFill(Color.rgb(196, 164, 114));
        gc.fillRoundRect(x + 8, y, w - 16, h, 3, 3);
        gc.setStroke(Color.rgb(120, 92, 50));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x + 8, y, w - 16, h, 3, 3);

        // Color bands
        double bandWidth = 3;
        Color[] bands = getColorBands();
        double bandStart = x + 11;
        for (int i = 0; i < bands.length; i++) {
            gc.setFill(bands[i]);
            gc.fillRect(bandStart + i * 5, y + 1, bandWidth, h - 2);
        }

        // Value label — light text reads against dark editor
        gc.setFill(Color.rgb(223, 225, 229));
        gc.setFont(Font.font("Monospaced", 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(resistance + "Ω", x + w / 2, y - 3);

        // Pins
        getPin("1").render(gc);
        getPin("2").render(gc);
    }

    private Color[] getColorBands() {
        // Simplified: just show brown for generic resistor
        return new Color[]{
                Color.rgb(165, 42, 42),   // Brown
                Color.rgb(165, 42, 42),   // Brown
                Color.BLACK,              // Black
                Color.GOLD                // Tolerance
        };
    }

    public int getResistance() {
        return resistance;
    }

    public void setResistance(int resistance) {
        this.resistance = resistance;
    }

    @Override
    public Component clone() {
        return new Resistor(resistance);
    }
}
