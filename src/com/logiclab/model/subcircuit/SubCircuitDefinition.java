package com.logiclab.model.subcircuit;

import com.logiclab.model.Circuit;
import com.logiclab.model.Component;
import com.logiclab.model.ports.InputPort;
import com.logiclab.model.ports.OutputPort;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory definition of a reusable sub-circuit. Pairs a fully realized
 * inner Circuit with a stable id (e.g. "user.halfadder.v1") and the list of
 * port components that expose its interface.
 */
public class SubCircuitDefinition {
    private final String id;
    private final String name;
    private final Circuit inner;

    public SubCircuitDefinition(String id, String name, Circuit inner) {
        this.id = id;
        this.name = name;
        this.inner = inner;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Circuit getInner() { return inner; }

    /** Input ports in placement order (top-to-bottom on the outer box). */
    public List<InputPort> inputPorts() {
        List<InputPort> out = new ArrayList<>();
        for (Component c : inner.getComponents()) {
            if (c instanceof InputPort ip) out.add(ip);
        }
        out.sort((a, b) -> Double.compare(a.getY(), b.getY()));
        return out;
    }

    /** Output ports in placement order (top-to-bottom on the outer box). */
    public List<OutputPort> outputPorts() {
        List<OutputPort> out = new ArrayList<>();
        for (Component c : inner.getComponents()) {
            if (c instanceof OutputPort op) out.add(op);
        }
        out.sort((a, b) -> Double.compare(a.getY(), b.getY()));
        return out;
    }
}
