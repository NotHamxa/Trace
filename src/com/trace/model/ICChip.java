package com.trace.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public abstract class ICChip extends Component {
    protected boolean powered;
    protected final int pinCount;
    protected final int vccPin;
    protected final int gndPin;

    public ICChip(String name) {
        this(name, 14, 14, 7);
    }

    public ICChip(String name, int pinCount, int vccPin, int gndPin) {
        super(name, pinCount * 10, 40);
        this.pinCount = pinCount;
        this.vccPin = vccPin;
        this.gndPin = gndPin;
        initPins();
    }

    private void initPins() {
        int half = pinCount / 2;
        // Bottom row: pins 1..half (left to right)
        for (int i = 1; i <= half; i++) {
            PinType type;
            if (i == gndPin) type = PinType.GROUND;
            else if (i == vccPin) type = PinType.POWER;
            else type = getDefaultPinType(i);
            addPin(new Pin(String.valueOf(i), type, this));
        }
        // Top row: pins (half+1)..pinCount
        for (int i = half + 1; i <= pinCount; i++) {
            PinType type;
            if (i == vccPin) type = PinType.POWER;
            else if (i == gndPin) type = PinType.GROUND;
            else type = getDefaultPinType(i);
            addPin(new Pin(String.valueOf(i), type, this));
        }
    }

    protected PinType getDefaultPinType(int pinNumber) {
        // Subclasses can override; default: determine from pin mapping
        return PinType.INPUT;
    }

    @Override
    protected void updatePinPositions() {
        double pinSpacing = 20;
        double startX = getX() + 10;
        int half = pinCount / 2;
        // Bottom row: pins 1..half (left to right)
        for (int i = 0; i < half; i++) {
            Pin p = getPin(String.valueOf(i + 1));
            if (p != null) {
                p.setPosition(startX + i * pinSpacing, getY() + getHeight());
            }
        }
        // Top row: highest pin leftmost, (half+1) rightmost
        for (int i = 0; i < half; i++) {
            Pin p = getPin(String.valueOf(pinCount - i));
            if (p != null) {
                p.setPosition(startX + i * pinSpacing, getY());
            }
        }
    }

    @Override
    public void simulate() {
        powered = (getPin(String.valueOf(vccPin)).getState() == LogicState.HIGH
                && getPin(String.valueOf(gndPin)).getState() == LogicState.LOW);

        if (!powered) {
            for (Pin p : getOutputPins()) {
                p.setState(LogicState.FLOATING);
            }
            return;
        }
        computeGates();
    }

    protected abstract void computeGates();
    protected abstract List<Pin> getOutputPins();

    public int getPinCount() {
        return pinCount;
    }

    /**
     * Returns a human-readable description of this IC's gate routing.
     * Each entry describes one internal gate: its inputs, output, and function.
     * Subclasses override to provide chip-specific info.
     */
    public List<String> getGateDescriptions() {
        return new ArrayList<>();
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();

        // Body
        gc.setFill(Color.rgb(40, 40, 40));
        gc.fillRect(x, y, w, h);
        gc.setStroke(Color.rgb(80, 80, 80));
        gc.setLineWidth(1.5);
        gc.strokeRect(x, y, w, h);

        // Notch at left center
        gc.setFill(Color.rgb(60, 60, 60));
        gc.fillArc(x - 3, y + h / 2 - 6, 10, 12, 270, 180, javafx.scene.shape.ArcType.ROUND);

        // Pin 1 dot indicator (bottom-left, near pin 1)
        gc.setFill(Color.WHITE);
        gc.fillOval(x + 5, y + h - 9, 4, 4);

        // Label
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospaced", 10));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(getName(), x + w / 2, y + h / 2 + 3);

        // Draw pins
        double pinSpacing = 20;
        double startX = x + 10;
        gc.setFont(Font.font("Monospaced", 8));

        int half = pinCount / 2;
        // Bottom pins (1..half). No leg stroke — just the pin state dot + label.
        for (int i = 0; i < half; i++) {
            Pin p = getPin(String.valueOf(i + 1));
            if (p != null) {
                double px = startX + i * pinSpacing;
                p.render(gc);
                gc.setFill(Color.LIGHTGRAY);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(String.valueOf(i + 1), px, y + h + 14);
            }
        }

        // Top pins (pinCount down to half+1, left to right). No leg stroke.
        for (int i = 0; i < half; i++) {
            int pinNum = pinCount - i;
            Pin p = getPin(String.valueOf(pinNum));
            if (p != null) {
                double px = startX + i * pinSpacing;
                p.render(gc);
                gc.setFill(Color.LIGHTGRAY);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(String.valueOf(pinNum), px, y - 6);
            }
        }
    }
}
