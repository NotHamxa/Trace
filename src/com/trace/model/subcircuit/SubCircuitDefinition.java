package com.trace.model.subcircuit;

import com.trace.model.Circuit;
import com.trace.model.Component;
import com.trace.model.ports.InputPort;
import com.trace.model.ports.OutputPort;

import java.util.ArrayList;
import java.util.List;

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

    public List<InputPort> inputPorts() {
        List<InputPort> out = new ArrayList<>();
        for (Component c : inner.getComponents()) {
            if (c instanceof InputPort ip) out.add(ip);
        }
        out.sort((a, b) -> Double.compare(a.getY(), b.getY()));
        return out;
    }

    public List<OutputPort> outputPorts() {
        List<OutputPort> out = new ArrayList<>();
        for (Component c : inner.getComponents()) {
            if (c instanceof OutputPort op) out.add(op);
        }
        out.sort((a, b) -> Double.compare(a.getY(), b.getY()));
        return out;
    }
}
