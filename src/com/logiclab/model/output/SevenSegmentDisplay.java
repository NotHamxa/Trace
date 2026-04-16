package com.logiclab.model.output;

import com.logiclab.model.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class SevenSegmentDisplay extends OutputComponent {
    // Segments: a(top), b(top-right), c(bottom-right), d(bottom), e(bottom-left), f(top-left), g(middle)
    private static final String[] SEGMENT_NAMES = {"a", "b", "c", "d", "e", "f", "g"};

    public SevenSegmentDisplay() {
        super("7-Seg Display", 50, 70);
        for (String seg : SEGMENT_NAMES) {
            addPin(new Pin(seg, PinType.INPUT, this));
        }
        addPin(new Pin("GND", PinType.GROUND, this));
    }

    @Override
    public void simulate() {
        // Visual state based on pin inputs
    }

    @Override
    public boolean isActive() {
        for (String seg : SEGMENT_NAMES) {
            if (getPin(seg).getState() == LogicState.HIGH) return true;
        }
        return false;
    }

    private boolean isSegmentOn(String segment) {
        return getPin(segment).getState() == LogicState.HIGH;
    }

    @Override
    protected void updatePinPositions() {
        double x = getX();
        double y = getY();
        // Pins along the bottom
        for (int i = 0; i < SEGMENT_NAMES.length; i++) {
            getPin(SEGMENT_NAMES[i]).setPosition(x + 5 + i * 6, y + getHeight());
        }
        getPin("GND").setPosition(x + getWidth() - 5, y + getHeight());
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();

        // Background
        gc.setFill(Color.rgb(20, 20, 20));
        gc.fillRoundRect(x, y, w, h, 4, 4);

        Color onColor = Color.RED;
        Color offColor = Color.rgb(50, 20, 20);

        double segW = 22;
        double segH = 4;
        double cx = x + w / 2;
        double cy = y + h / 2;

        // a - top horizontal
        gc.setFill(isSegmentOn("a") ? onColor : offColor);
        gc.fillRect(cx - segW / 2, y + 8, segW, segH);

        // b - top right vertical
        gc.setFill(isSegmentOn("b") ? onColor : offColor);
        gc.fillRect(cx + segW / 2 - segH, y + 8, segH, segW * 0.7);

        // c - bottom right vertical
        gc.setFill(isSegmentOn("c") ? onColor : offColor);
        gc.fillRect(cx + segW / 2 - segH, cy + 2, segH, segW * 0.7);

        // d - bottom horizontal
        gc.setFill(isSegmentOn("d") ? onColor : offColor);
        gc.fillRect(cx - segW / 2, y + h - 12, segW, segH);

        // e - bottom left vertical
        gc.setFill(isSegmentOn("e") ? onColor : offColor);
        gc.fillRect(cx - segW / 2, cy + 2, segH, segW * 0.7);

        // f - top left vertical
        gc.setFill(isSegmentOn("f") ? onColor : offColor);
        gc.fillRect(cx - segW / 2, y + 8, segH, segW * 0.7);

        // g - middle horizontal
        gc.setFill(isSegmentOn("g") ? onColor : offColor);
        gc.fillRect(cx - segW / 2, cy - segH / 2, segW, segH);

        // Render pins
        for (Pin p : getPins()) {
            p.render(gc);
        }
    }

    @Override
    public Component clone() {
        return new SevenSegmentDisplay();
    }
}
