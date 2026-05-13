package com.trace.model.ports;

import com.trace.model.Component;
import com.trace.model.LogicState;
import com.trace.model.Pin;
import com.trace.model.PinType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class InputPort extends Component {
    private String portLabel;
    private LogicState externalState = LogicState.FLOATING;

    public InputPort() {
        this("IN");
    }

    public InputPort(String label) {
        super("Input Port", 40, 30);
        this.portLabel = label == null ? "IN" : label;
        addPin(new Pin("OUT", PinType.OUTPUT, this));
    }

    public String getPortLabel() { return portLabel; }
    public void setPortLabel(String label) { this.portLabel = label == null ? "" : label; }

    public void setExternalState(LogicState s) {
        this.externalState = s == null ? LogicState.FLOATING : s;
    }

    @Override
    public void simulate() {
        getPin("OUT").setState(externalState);
    }

    @Override
    protected void updatePinPositions() {
        getPin("OUT").setPosition(getX() + getWidth(), getY() + getHeight() / 2);
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX(), y = getY(), w = getWidth(), h = getHeight();

        gc.setFill(Color.rgb(40, 60, 90));
        gc.fillRoundRect(x, y, w, h, 4, 4);
        gc.setStroke(Color.rgb(80, 140, 220));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(x, y, w, h, 4, 4);

        gc.setFill(Color.rgb(223, 225, 229));
        gc.setFont(Font.font("Monospaced", 10));
        gc.setTextAlign(TextAlignment.CENTER);
        String label = portLabel == null || portLabel.isEmpty() ? "IN" : portLabel;
        gc.fillText(label, x + w / 2, y + h / 2 + 4);

        gc.setStroke(Color.rgb(80, 140, 220));
        gc.setLineWidth(1.5);
        gc.strokeLine(x + w - 6, y + h / 2 - 3, x + w - 2, y + h / 2);
        gc.strokeLine(x + w - 6, y + h / 2 + 3, x + w - 2, y + h / 2);

        getPin("OUT").render(gc);
    }

    @Override
    public Component clone() {
        return new InputPort(portLabel);
    }
}
