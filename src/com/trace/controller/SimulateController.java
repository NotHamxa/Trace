package com.trace.controller;

import com.trace.exceptions.CircuitShortException;
import com.trace.exceptions.OscillationException;
import com.trace.model.Circuit;
import com.trace.util.CircuitValidator;

import java.util.List;

public class SimulateController {
    private Circuit circuit;

    public SimulateController(Circuit circuit) {
        this.circuit = circuit;
    }

    public List<String> startSimulation() {
        List<String> warnings = CircuitValidator.validate(circuit);
        circuit.simulate();
        return warnings;
    }

    public void setCircuit(Circuit circuit) {
        this.circuit = circuit;
    }
}
