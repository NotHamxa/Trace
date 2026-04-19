package com.trace.model.input;

import com.trace.model.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class PushButton extends InputComponent {
    private boolean pressed = false;

    public PushButton() {
        super("Push Button", 35, 35);
        addPin(new Pin("OUT", PinType.OUTPUT, this));
    }

    @Override
    public void onInteract() {
        pressed = true;
        outputState = LogicState.HIGH;
    }

    public void onRelease() {
        pressed = false;
        outputState = LogicState.LOW;
    }

    public boolean isPressed() {
        return pressed;
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
        double cx = x + w / 2;
        double cy = y + h / 2;

        // Housing — dark chrome
        gc.setFill(Color.rgb(23, 24, 26));
        gc.fillRoundRect(x, y, w, h, 6, 6);
        gc.setStroke(Color.rgb(60, 63, 65));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, w, h, 6, 6);

        // Cap
        double inset = 5;
        double capW = w - inset * 2;
        double capH = h - inset * 2;
        if (pressed) {
            // Glow halo
            gc.setFill(Color.rgb(219, 92, 92, 0.28));
            gc.fillOval(cx - capW * 0.85, cy - capH * 0.85, capW * 1.7, capH * 1.7);
            gc.setFill(Color.rgb(219, 92, 92));
        } else {
            gc.setFill(Color.rgb(46, 48, 51));
        }
        gc.fillOval(x + inset, y + inset, capW, capH);
        gc.setStroke(Color.rgb(15, 16, 18));
        gc.setLineWidth(1);
        gc.strokeOval(x + inset, y + inset, capW, capH);

        // Specular highlight on the cap
        gc.setFill(pressed ? Color.rgb(255, 190, 190, 0.35) : Color.rgb(223, 225, 229, 0.12));
        gc.fillOval(cx - capW * 0.28, cy - capH * 0.34, capW * 0.42, capH * 0.28);

        // Label
        gc.setFill(Color.rgb(134, 138, 145));
        gc.setFont(Font.font("Monospaced", 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("BTN", x + w / 2, y + h + 11);

        // Output pin
        getPin("OUT").render(gc);
    }

    @Override
    public Component clone() {
        return new PushButton();
    }
}
