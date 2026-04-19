package com.trace.model.power;

import com.trace.model.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class Ground extends PowerComponent {

    public Ground() {
        super("Ground", 35, 30);
        addPin(new Pin("GND", PinType.GROUND, this));
    }

    @Override
    public void simulate() {
        getPin("GND").setState(LogicState.LOW);
    }

    @Override
    protected void updatePinPositions() {
        getPin("GND").setPosition(getX() + getWidth() / 2, getY());
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        double cx = x + w / 2;

        // Wire from pin down — light silver so it reads on dark bg
        gc.setStroke(Color.rgb(200, 203, 208));
        gc.setLineWidth(2);
        gc.strokeLine(cx, y, cx, y + 8);

        // Ground symbol (3 horizontal lines, decreasing width) — light gray
        gc.setStroke(Color.rgb(223, 225, 229));
        gc.setLineWidth(2.5);
        gc.strokeLine(cx - 14, y + 10, cx + 14, y + 10);
        gc.setLineWidth(2);
        gc.strokeLine(cx - 9, y + 16, cx + 9, y + 16);
        gc.setLineWidth(1.5);
        gc.strokeLine(cx - 4, y + 22, cx + 4, y + 22);

        // Label
        gc.setFill(Color.rgb(134, 138, 145));
        gc.setFont(Font.font("Monospaced", 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("GND", cx, y + h + 5);

        // Pin
        getPin("GND").render(gc);
    }

    @Override
    public Component clone() {
        return new Ground();
    }
}
