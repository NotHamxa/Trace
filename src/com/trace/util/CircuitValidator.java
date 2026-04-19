package com.trace.util;

import com.trace.exceptions.CircuitShortException;
import com.trace.model.*;

import java.util.ArrayList;
import java.util.List;

public class CircuitValidator {

    public static List<String> validate(Circuit circuit) {
        List<String> warnings = new ArrayList<>();

        // 1. Check for short circuits
        for (Breadboard bb : circuit.getBreadboards()) {
            for (Net net : bb.getAllNets()) {
                boolean hasHigh = false, hasLow = false;
                for (Pin p : net.getConnectedPins()) {
                    if (p.getType() == PinType.POWER) hasHigh = true;
                    if (p.getType() == PinType.GROUND) hasLow = true;
                }
                if (hasHigh && hasLow) {
                    throw new CircuitShortException("Short circuit detected: power and ground on same net");
                }
            }
        }

        // 2. Check for floating IC inputs
        for (Component c : circuit.getComponents()) {
            if (c instanceof ICChip) {
                for (Pin p : c.getPins()) {
                    if (p.getType() == PinType.INPUT && !isConnected(p, circuit)) {
                        warnings.add("Floating input: " + c.getName() + " pin " + p.getLabel());
                    }
                }
            }
        }

        // 3. Check for unpowered ICs
        for (Component c : circuit.getComponents()) {
            if (c instanceof ICChip) {
                Pin vcc = c.getPin("14");
                Pin gnd = c.getPin("7");
                if (!isConnected(vcc, circuit) || !isConnected(gnd, circuit)) {
                    warnings.add(c.getName() + " is not connected to power");
                }
            }
        }

        return warnings;
    }

    private static boolean isConnected(Pin pin, Circuit circuit) {
        for (Wire w : circuit.getWires()) {
            if (w.getStartPin() == pin || w.getEndPin() == pin) return true;
        }
        return false;
    }
}
