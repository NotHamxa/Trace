package com.trace.model.output;

import com.trace.model.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class LED extends OutputComponent {
    private transient Color color;
    private double colorR, colorG, colorB;

    public LED(Color color) {
        super("LED", 20, 30);
        this.color = color;
        this.colorR = color.getRed();
        this.colorG = color.getGreen();
        this.colorB = color.getBlue();
        addPin(new Pin("anode", PinType.INPUT, this));
        addPin(new Pin("cathode", PinType.GROUND, this));
    }

    @Override
    public void simulate() {
        // LED is active — visual state is based on isActive()
    }

    @Override
    public boolean isActive() {
        return getPin("anode").getState() == LogicState.HIGH;
    }

    @Override
    protected void updatePinPositions() {
        getPin("anode").setPosition(getX() + getWidth() / 2 - 5, getY() + getHeight());
        getPin("cathode").setPosition(getX() + getWidth() / 2 + 5, getY() + getHeight());
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight() - 10;  // Top portion is the LED dome

        double centerX = x + w / 2;
        double centerY = y + h / 2;

        if (isActive()) {
            // Glow effect
            gc.setFill(getColor().deriveColor(0, 1, 1, 0.3));
            gc.fillOval(centerX - w, centerY - w, w * 2, w * 2);

            // Bright LED
            gc.setFill(getColor());
        } else {
            // Dim LED
            gc.setFill(getColor().deriveColor(0, 0.3, 0.4, 1));
        }

        gc.fillOval(x, y, w, h);
        gc.setStroke(Color.rgb(80, 80, 80));
        gc.setLineWidth(1);
        gc.strokeOval(x, y, w, h);

        // Flat bottom (cathode indicator)
        gc.setStroke(Color.rgb(100, 100, 100));
        gc.strokeLine(x + 2, y + h - 2, x + w - 2, y + h - 2);

        // Leads
        gc.setStroke(Color.SILVER);
        gc.setLineWidth(1.5);
        gc.strokeLine(centerX - 5, y + h, centerX - 5, y + getHeight());
        gc.strokeLine(centerX + 5, y + h, centerX + 5, y + getHeight());

        // Pin labels
        gc.setFill(Color.GRAY);
        gc.setFont(Font.font("Monospaced", 7));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("+", centerX - 5, y + getHeight() + 9);
        gc.fillText("-", centerX + 5, y + getHeight() + 9);

        // Render pins
        getPin("anode").render(gc);
        getPin("cathode").render(gc);
    }

    public Color getColor() {
        if (color == null) color = new Color(colorR, colorG, colorB, 1.0);
        return color;
    }

    @Override
    public Component clone() {
        return new LED(getColor());
    }
}
