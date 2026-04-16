package com.logiclab.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public abstract class ICChip extends Component {
    protected boolean powered;

    public ICChip(String name) {
        super(name, 140, 40);
        initPins();
    }

    private void initPins() {
        // 14-pin DIP: pins 1-7 on the left, pins 8-14 on the right
        for (int i = 1; i <= 7; i++) {
            PinType type;
            if (i == 7) {
                type = PinType.GROUND;
            } else {
                type = getDefaultPinType(i);
            }
            addPin(new Pin(String.valueOf(i), type, this));
        }
        for (int i = 8; i <= 14; i++) {
            PinType type;
            if (i == 14) {
                type = PinType.POWER;
            } else {
                type = getDefaultPinType(i);
            }
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
        // Bottom row: pins 1-7 (left to right)
        for (int i = 0; i < 7; i++) {
            Pin p = getPin(String.valueOf(i + 1));
            if (p != null) {
                p.setPosition(startX + i * pinSpacing, getY() + getHeight());
            }
        }
        // Top row: pins 14-8 (left to right → pin 14 leftmost, pin 8 rightmost)
        for (int i = 0; i < 7; i++) {
            Pin p = getPin(String.valueOf(14 - i));
            if (p != null) {
                p.setPosition(startX + i * pinSpacing, getY());
            }
        }
    }

    @Override
    public void simulate() {
        powered = (getPin("14").getState() == LogicState.HIGH
                && getPin("7").getState() == LogicState.LOW);

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

        // Bottom pins (1-7). No leg stroke — just the pin state dot + label.
        for (int i = 0; i < 7; i++) {
            Pin p = getPin(String.valueOf(i + 1));
            if (p != null) {
                double px = startX + i * pinSpacing;
                p.render(gc);
                gc.setFill(Color.LIGHTGRAY);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(String.valueOf(i + 1), px, y + h + 14);
            }
        }

        // Top pins (14 down to 8, left to right). No leg stroke.
        for (int i = 0; i < 7; i++) {
            int pinNum = 14 - i;
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
