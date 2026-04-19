package com.trace.model.input;

import com.trace.model.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class ToggleSwitch extends InputComponent {
    private boolean on = false;

    public ToggleSwitch() {
        super("Toggle Switch", 50, 30);
        addPin(new Pin("OUT", PinType.OUTPUT, this));
    }

    @Override
    public void onInteract() {
        on = !on;
        outputState = on ? LogicState.HIGH : LogicState.LOW;
    }

    @Override
    public Pin getOutputPin() {
        return getPin("OUT");
    }

    @Override
    protected void updatePinPositions() {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        Pin p = getPin("OUT");
        if (p == null) return;
        switch (getPinSide()) {
            case BOTTOM: p.setPosition(x + w / 2, y + h); break;
            case LEFT:   p.setPosition(x, y + h / 2);     break;
            case RIGHT:  p.setPosition(x + w, y + h / 2); break;
            case TOP:
            default:     p.setPosition(x + w / 2, y);     break;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();

        // Housing — dark with accent border when ON
        gc.setFill(Color.rgb(23, 24, 26));
        gc.fillRoundRect(x, y, w, h, h / 2, h / 2);
        gc.setStroke(on ? Color.rgb(53, 116, 240) : Color.rgb(60, 63, 65));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(x, y, w, h, h / 2, h / 2);

        // Soft inner tint when ON (accent blue glow inside the track)
        if (on) {
            gc.setFill(Color.rgb(53, 116, 240, 0.18));
            gc.fillRoundRect(x + 1, y + 1, w - 2, h - 2, (h - 2) / 2, (h - 2) / 2);
        }

        // Toggle knob
        double knobPad = 4;
        double knobSize = h - knobPad * 2;
        double knobX = on ? x + w - knobSize - knobPad : x + knobPad;
        gc.setFill(on ? Color.rgb(53, 116, 240) : Color.rgb(134, 138, 145));
        gc.fillOval(knobX, y + knobPad, knobSize, knobSize);
        gc.setStroke(on ? Color.rgb(100, 150, 255) : Color.rgb(95, 98, 103));
        gc.setLineWidth(0.8);
        gc.strokeOval(knobX, y + knobPad, knobSize, knobSize);

        // Label
        gc.setFill(on ? Color.rgb(53, 116, 240) : Color.rgb(134, 138, 145));
        gc.setFont(Font.font("Monospaced", 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(on ? "ON" : "OFF", x + w / 2, y + h + 11);

        // Output pin
        getPin("OUT").render(gc);
    }

    public boolean isOn() {
        return on;
    }

    /** Programmatically sets the switch state (used by test mode). */
    public void setState(boolean value) {
        on = value;
        outputState = on ? LogicState.HIGH : LogicState.LOW;
    }

    @Override
    public Component clone() {
        return new ToggleSwitch();
    }
}
