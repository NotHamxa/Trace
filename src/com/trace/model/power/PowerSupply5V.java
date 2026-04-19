package com.trace.model.power;

import com.trace.model.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class PowerSupply5V extends PowerComponent {

    public PowerSupply5V() {
        super("5V Supply", 35, 30);
        addPin(new Pin("VCC", PinType.POWER, this));
    }

    @Override
    public void simulate() {
        getPin("VCC").setState(LogicState.HIGH);
    }

    @Override
    protected void updatePinPositions() {
        getPin("VCC").setPosition(getX() + getWidth() / 2, getY() + getHeight());
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();

        // Dark housing with red accent border
        gc.setFill(Color.rgb(23, 24, 26));
        gc.fillRoundRect(x, y, w, h, 4, 4);
        gc.setStroke(Color.rgb(219, 92, 92));
        gc.setLineWidth(1.4);
        gc.strokeRoundRect(x, y, w, h, 4, 4);

        // Red accent strip at the top to read as "power"
        gc.setFill(Color.rgb(219, 92, 92, 0.35));
        gc.fillRect(x + 2, y + 2, w - 4, 4);

        // Label — red on dark
        gc.setFill(Color.rgb(219, 92, 92));
        gc.setFont(Font.font("Monospaced", 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("+5V", x + w / 2, y + h / 2 + 6);

        // Wire down to pin
        gc.setStroke(Color.rgb(219, 92, 92));
        gc.setLineWidth(2);
        gc.strokeLine(x + w / 2, y + h, x + w / 2, y + h + 5);

        // Pin
        getPin("VCC").render(gc);
    }

    @Override
    public Component clone() {
        return new PowerSupply5V();
    }
}
