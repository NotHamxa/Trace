package com.logiclab.model.subcircuit;

import com.logiclab.model.Component;
import com.logiclab.model.LogicState;
import com.logiclab.model.Pin;
import com.logiclab.model.PinType;
import com.logiclab.model.ports.InputPort;
import com.logiclab.model.ports.OutputPort;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * A placed reference to a saved SubCircuitDefinition. Renders as a box with
 * external pins matching the definition's input/output ports; simulating it
 * runs the inner circuit and shuttles signals through the ports.
 *
 * Pin labels exposed externally match the port labels ("A", "Cin", "SUM", …).
 * If the backing definition is missing (file gone, broken reference), the
 * instance renders as a red placeholder and simulate() is a no-op.
 */
public class SubCircuitInstance extends Component {
    private static final double ROW_H = 22;
    private static final double HEADER_H = 20;
    private static final double WIDTH = 110;

    private final String ref;
    private final SubCircuitDefinition definition;
    private final boolean broken;
    private final java.util.List<Pin> inPins = new java.util.ArrayList<>();
    private final java.util.List<Pin> outPins = new java.util.ArrayList<>();

    public SubCircuitInstance(String ref, SubCircuitDefinition definition) {
        super(displayName(ref, definition), WIDTH, heightFor(definition));
        this.ref = ref;
        this.definition = definition;
        this.broken = (definition == null);

        if (definition != null) {
            int i = 0;
            for (InputPort ip : definition.inputPorts()) {
                Pin p = new Pin(uniqueLabel(ip.getPortLabel(), "IN", i++), PinType.INPUT, this);
                addPin(p);
                inPins.add(p);
            }
            int o = 0;
            for (OutputPort op : definition.outputPorts()) {
                Pin p = new Pin(uniqueLabel(op.getPortLabel(), "OUT", o++), PinType.OUTPUT, this);
                addPin(p);
                outPins.add(p);
            }
        }
    }

    private static String uniqueLabel(String base, String fallback, int idx) {
        String s = (base == null || base.isBlank()) ? fallback : base;
        return s + "#" + idx;
    }

    public String getRef() { return ref; }
    public SubCircuitDefinition getDefinition() { return definition; }
    public boolean isBroken() { return broken; }

    private static String displayName(String ref, SubCircuitDefinition def) {
        if (def != null) return def.getName();
        return "[missing] " + ref;
    }

    private static double heightFor(SubCircuitDefinition def) {
        if (def == null) return HEADER_H + ROW_H;
        int rows = Math.max(1, Math.max(def.inputPorts().size(), def.outputPorts().size()));
        return HEADER_H + rows * ROW_H + 6;
    }

    @Override
    public void simulate() {
        if (broken) return;

        List<InputPort> ins = definition.inputPorts();
        for (int i = 0; i < ins.size() && i < inPins.size(); i++) {
            ins.get(i).setExternalState(inPins.get(i).getState());
        }

        definition.getInner().simulate();

        List<OutputPort> outs = definition.outputPorts();
        for (int i = 0; i < outs.size() && i < outPins.size(); i++) {
            outPins.get(i).setState(outs.get(i).readState());
        }
    }

    @Override
    protected void updatePinPositions() {
        if (broken) return;
        double x = getX(), y = getY(), w = getWidth();
        for (int i = 0; i < inPins.size(); i++) {
            inPins.get(i).setPosition(x, y + HEADER_H + i * ROW_H + ROW_H / 2);
        }
        for (int i = 0; i < outPins.size(); i++) {
            outPins.get(i).setPosition(x + w, y + HEADER_H + i * ROW_H + ROW_H / 2);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX(), y = getY(), w = getWidth(), h = getHeight();

        gc.setFill(broken ? Color.rgb(60, 30, 30) : Color.rgb(46, 48, 51));
        gc.fillRoundRect(x, y, w, h, 6, 6);
        gc.setStroke(broken ? Color.rgb(219, 92, 92) : Color.rgb(95, 98, 103));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(x, y, w, h, 6, 6);

        // Header
        gc.setFill(broken ? Color.rgb(219, 92, 92, 0.25) : Color.rgb(53, 116, 240, 0.25));
        gc.fillRoundRect(x, y, w, HEADER_H, 6, 6);
        gc.setFill(Color.rgb(223, 225, 229));
        gc.setFont(Font.font("Monospaced", 10));
        gc.setTextAlign(TextAlignment.CENTER);
        String title = broken ? "[missing]" : definition.getName();
        gc.fillText(title, x + w / 2, y + HEADER_H - 6);

        if (broken) {
            gc.setFont(Font.font("Monospaced", 8));
            gc.setFill(Color.rgb(200, 120, 120));
            gc.fillText(ref, x + w / 2, y + h - 6);
            return;
        }

        // Pin labels inside the box
        gc.setFont(Font.font("Monospaced", 9));
        gc.setFill(Color.rgb(200, 203, 208));
        List<InputPort> ins = definition.inputPorts();
        List<OutputPort> outs = definition.outputPorts();
        gc.setTextAlign(TextAlignment.LEFT);
        for (int i = 0; i < ins.size(); i++) {
            gc.fillText(ins.get(i).getPortLabel(), x + 6, y + HEADER_H + i * ROW_H + ROW_H / 2 + 3);
        }
        gc.setTextAlign(TextAlignment.RIGHT);
        for (int i = 0; i < outs.size(); i++) {
            gc.fillText(outs.get(i).getPortLabel(), x + w - 6, y + HEADER_H + i * ROW_H + ROW_H / 2 + 3);
        }

        for (Pin p : getPins()) p.render(gc);
    }

    @Override
    public Component clone() {
        return new SubCircuitInstance(ref, definition);
    }
}
