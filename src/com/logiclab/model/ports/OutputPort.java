package com.logiclab.model.ports;

import com.logiclab.model.Component;
import com.logiclab.model.LogicState;
import com.logiclab.model.Pin;
import com.logiclab.model.PinType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Marks an external output of a sub-circuit. Placed inside the inner circuit
 * while designing a sub-circuit. Whatever drives this port's input pin inside
 * the inner circuit is what the outer box exposes on its matching output pin.
 */
public class OutputPort extends Component {
    private String portLabel;

    public OutputPort() {
        this("OUT");
    }

    public OutputPort(String label) {
        super("Output Port", 40, 30);
        this.portLabel = label == null ? "OUT" : label;
        addPin(new Pin("IN", PinType.INPUT, this));
    }

    public String getPortLabel() { return portLabel; }
    public void setPortLabel(String label) { this.portLabel = label == null ? "" : label; }

    /** Called by SubCircuitInstance after inner simulate(). */
    public LogicState readState() {
        return getPin("IN").getState();
    }

    @Override
    public void simulate() {
        // Nothing to drive — state flows IN via whatever inner component wires here.
    }

    @Override
    protected void updatePinPositions() {
        getPin("IN").setPosition(getX(), getY() + getHeight() / 2);
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX(), y = getY(), w = getWidth(), h = getHeight();

        gc.setFill(Color.rgb(60, 50, 40));
        gc.fillRoundRect(x, y, w, h, 4, 4);
        gc.setStroke(Color.rgb(230, 160, 80));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(x, y, w, h, 4, 4);

        gc.setFill(Color.rgb(223, 225, 229));
        gc.setFont(Font.font("Monospaced", 10));
        gc.setTextAlign(TextAlignment.CENTER);
        String label = portLabel == null || portLabel.isEmpty() ? "OUT" : portLabel;
        gc.fillText(label, x + w / 2, y + h / 2 + 4);

        // Arrow hint pointing left (signal flows in)
        gc.setStroke(Color.rgb(230, 160, 80));
        gc.setLineWidth(1.5);
        gc.strokeLine(x + 2, y + h / 2, x + 6, y + h / 2 - 3);
        gc.strokeLine(x + 2, y + h / 2, x + 6, y + h / 2 + 3);

        getPin("IN").render(gc);
    }

    @Override
    public Component clone() {
        return new OutputPort(portLabel);
    }
}
