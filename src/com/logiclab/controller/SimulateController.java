package com.logiclab.controller;

import com.logiclab.exceptions.CircuitShortException;
import com.logiclab.exceptions.OscillationException;
import com.logiclab.model.Circuit;
import com.logiclab.util.CircuitValidator;

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
