package com.trace.model.ports;

import com.trace.model.Component;
import com.trace.model.LogicState;
import com.trace.model.Pin;
import com.trace.model.PinType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Marks an external input to a sub-circuit. Placed inside the inner circuit
 * while designing a sub-circuit. When the sub-circuit is instantiated on
 * another canvas, the outer box grows a new input pin for each InputPort,
 * and the SubCircuitInstance feeds the outer pin state into this port's
 * output on each simulate().
 *
 * The label is what shows up on the outer box. "A", "Cin", "CLK", etc.
 */
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

    /** Called by SubCircuitInstance before inner simulate(). */
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

        // Arrow hint pointing right (signal flows out)
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
