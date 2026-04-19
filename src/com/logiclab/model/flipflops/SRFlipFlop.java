package com.logiclab.model.flipflops;

import com.logiclab.model.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class SRFlipFlop extends FlipFlop {
    private boolean q = false;

    public SRFlipFlop() {
        super("SR Flip-Flop", 70, 60);
        addPin(new Pin("S", PinType.INPUT, this));
        addPin(new Pin("R", PinType.INPUT, this));
        addPin(new Pin("Q", PinType.OUTPUT, this));
        addPin(new Pin("Q'", PinType.OUTPUT, this));
    }

    @Override
    public void simulate() {
        LogicState s = getPin("S").getState();
        LogicState r = getPin("R").getState();

        if (s == LogicState.HIGH && r == LogicState.HIGH) {
            // Invalid — NOR-latch style: force both outputs LOW
            getPin("Q").setState(LogicState.LOW);
            getPin("Q'").setState(LogicState.LOW);
            return;
        }
        if (s == LogicState.HIGH) q = true;
        else if (r == LogicState.HIGH) q = false;
        // else: hold previous state

        getPin("Q").setState(q ? LogicState.HIGH : LogicState.LOW);
        getPin("Q'").setState(q ? LogicState.LOW : LogicState.HIGH);
    }

    @Override
    protected void updatePinPositions() {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        getPin("S").setPosition(x,     y + h * 0.30);
        getPin("R").setPosition(x,     y + h * 0.70);
        getPin("Q").setPosition(x + w, y + h * 0.30);
        getPin("Q'").setPosition(x + w, y + h * 0.70);
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();

        // Body
        gc.setFill(Color.rgb(46, 48, 51));
        gc.fillRoundRect(x, y, w, h, 6, 6);
        gc.setStroke(Color.rgb(95, 98, 103));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(x, y, w, h, 6, 6);

        // Title bar
        gc.setFill(Color.rgb(53, 116, 240, 0.25));
        gc.fillRoundRect(x, y, w, 14, 6, 6);
        gc.setFill(Color.rgb(223, 225, 229));
        gc.setFont(Font.font("Monospaced", 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("SR", x + w / 2, y + 10);

        // Pin labels
        gc.setFill(Color.rgb(200, 203, 208));
        gc.setFont(Font.font("Monospaced", 9));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("S",  x + 4, y + h * 0.30 + 3);
        gc.fillText("R",  x + 4, y + h * 0.70 + 3);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("Q",  x + w - 4, y + h * 0.30 + 3);
        gc.fillText("Q'", x + w - 4, y + h * 0.70 + 3);

        // Pins
        getPin("S").render(gc);
        getPin("R").render(gc);
        getPin("Q").render(gc);
        getPin("Q'").render(gc);
    }

    @Override
    public Component clone() {
        return new SRFlipFlop();
    }
}
