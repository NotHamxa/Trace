package com.trace.model;

import com.trace.exceptions.OscillationException;
import com.trace.model.flipflops.FlipFlop;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Circuit implements Serializable {
    @SuppressWarnings("unused")
    private Breadboard breadboard;
    private List<Breadboard> breadboards;
    private List<Component> components;
    private List<Wire> wires;
    private String name;
    private boolean modified;

    private List<LogicState[]> savedTestInputs;
    private List<LogicState[]> savedTestExpected;

    public Circuit() {
        this.breadboards = new ArrayList<>();
        this.breadboards.add(new Breadboard(30));
        this.components = new ArrayList<>();
        this.wires = new ArrayList<>();
        this.name = "Untitled";
        this.modified = false;
    }

    public void replaceBreadboards(List<Breadboard> bbs) {
        this.breadboards = new ArrayList<>(bbs);
    }

    public void addBreadboard(Breadboard bb) {
        breadboards.add(bb);
        modified = true;
    }

    public void removeBreadboard(Breadboard bb) {
        if (breadboards.size() > 1) {
            breadboards.remove(bb);
            modified = true;
        }
    }

    public void addComponent(Component c) {
        components.add(c);
        c.setPlaced(true);
        modified = true;
    }

    public void removeComponent(Component c) {
        components.remove(c);
        List<Wire> toRemove = new ArrayList<>();
        for (Wire w : wires) {
            if (w.getStartPin().getOwner() == c || w.getEndPin().getOwner() == c) {
                toRemove.add(w);
            }
        }
        wires.removeAll(toRemove);
        modified = true;
    }

    public void addWire(Wire wire) {
        wires.add(wire);
        modified = true;
    }

    public void removeWire(Wire w) {
        wires.remove(w);
        modified = true;
    }

    public void resetStates() {
        resetAllNets();
    }

    public void simulate() {
        resetAllNets();

        for (Component c : components) {
            if (c instanceof PowerComponent) {
                c.simulate();
            }
        }

        propagateNets();

        for (Component c : components) {
            if (c instanceof InputComponent
                    || c instanceof com.trace.model.ports.InputPort) {
                c.simulate();
            }
        }

        propagateNets();

        boolean changed = true;
        int maxIterations = 20;
        int iteration = 0;

        while (changed && iteration < maxIterations) {
            changed = false;
            for (Component c : components) {
                if (c instanceof ICChip || c instanceof FlipFlop || c instanceof com.trace.model.subcircuit.SubCircuitInstance) {
                    Map<Pin, LogicState> before = captureOutputStates(c);
                    c.simulate();
                    if (outputsChanged(c, before)) {
                        changed = true;
                    }
                }
            }
            propagateNets();
            iteration++;
        }

        if (iteration >= maxIterations) {
            StringBuilder detail = new StringBuilder("Unstable feedback loop detected. ");
            detail.append("The following ICs did not converge after ")
                  .append(maxIterations).append(" iterations: ");
            boolean first = true;
            for (Component c : components) {
                if (c instanceof ICChip || c instanceof FlipFlop || c instanceof com.trace.model.subcircuit.SubCircuitInstance) {
                    Map<Pin, LogicState> before = captureOutputStates(c);
                    c.simulate();
                    propagateNets();
                    if (outputsChanged(c, before)) {
                        if (!first) detail.append(", ");
                        detail.append(c.getName()).append(" (").append(c.getId()).append(")");
                        first = false;
                    }
                }
            }
            if (first) {
                detail.append("(could not identify specific ICs)");
            }
            throw new OscillationException(detail.toString());
        }

        for (Component c : components) {
            if (c instanceof OutputComponent) {
                c.simulate();
            }
        }

        for (Component c : components) {
            if (c instanceof PassiveComponent) {
                c.simulate();
            }
        }

        propagateNets();
    }

    private void resetAllNets() {
        for (Breadboard bb : breadboards) {
            for (Net net : bb.getAllNets()) {
                net.setState(LogicState.FLOATING);
                for (Pin p : net.getConnectedPins()) {
                    p.setState(LogicState.FLOATING);
                }
            }
        }
        for (Component c : components) {
            for (Pin p : c.getPins()) {
                p.setState(LogicState.FLOATING);
            }
        }
        for (Wire w : wires) {
            w.resetState();
        }
    }

    private void propagateNets() {
        for (int pass = 0; pass < 5; pass++) {
            for (Wire w : wires) {
                w.propagate();
            }
            for (Breadboard bb : breadboards) {
                bb.propagateAllNets();
            }
        }
    }

    private Map<Pin, LogicState> captureOutputStates(Component c) {
        Map<Pin, LogicState> states = new HashMap<>();
        for (Pin p : c.getPins()) {
            if (p.getType() == PinType.OUTPUT) {
                states.put(p, p.getState());
            }
        }
        return states;
    }

    private boolean outputsChanged(Component c, Map<Pin, LogicState> before) {
        for (Map.Entry<Pin, LogicState> entry : before.entrySet()) {
            if (entry.getKey().getState() != entry.getValue()) {
                return true;
            }
        }
        return false;
    }

    public List<Breadboard> getBreadboards() {
        return breadboards;
    }

    public Breadboard getBreadboard() {
        return breadboards.get(0);
    }

    public List<Component> getComponents() {
        return components;
    }

    public List<Wire> getWires() {
        return wires;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public List<LogicState[]> getSavedTestInputs() {
        return savedTestInputs;
    }

    public List<LogicState[]> getSavedTestExpected() {
        return savedTestExpected;
    }

    public void setSavedTests(List<LogicState[]> inputs, List<LogicState[]> expected) {
        this.savedTestInputs = inputs;
        this.savedTestExpected = expected;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        if (breadboards == null) {
            breadboards = new ArrayList<>();
            if (breadboard != null) {
                breadboards.add(breadboard);
                breadboard = null;
            } else {
                breadboards.add(new Breadboard(30));
            }
        }
    }

    public void saveToFile(File file) throws IOException {
        com.trace.io.CircuitJsonWriter.write(this, file);
    }

    public static Circuit loadFromFile(File file) throws IOException {
        return com.trace.io.CircuitJsonReader.read(file);
    }
}
