package com.trace.model.chips;

import com.trace.model.*;

import java.util.List;

/**
 * 7410 — Triple 3-input NAND gate (DIP-14).
 *
 * Pin layout:
 *   Gate 1: inputs 1, 2, 13 → output 12
 *   Gate 2: inputs 3, 4, 5  → output 6
 *   Gate 3: inputs 9, 10, 11 → output 8
 *   Pin 7: GND, Pin 14: Vcc
 */
public class NANDGate7410 extends ICChip {

    public NANDGate7410() {
        super("7410 NAND3");
    }

    @Override
    protected PinType getDefaultPinType(int pinNumber) {
        if (pinNumber == 12 || pinNumber == 6 || pinNumber == 8) {
            return PinType.OUTPUT;
        }
        return PinType.INPUT;
    }

    @Override
    protected void computeGates() {
        applyGate("1", "2", "13", "12");
        applyGate("3", "4", "5", "6");
        applyGate("9", "10", "11", "8");
    }

    private void applyGate(String inA, String inB, String inC, String out) {
        LogicState a = getPin(inA).getState();
        LogicState b = getPin(inB).getState();
        LogicState c = getPin(inC).getState();

        if (a == LogicState.FLOATING || b == LogicState.FLOATING || c == LogicState.FLOATING) {
            getPin(out).setState(LogicState.FLOATING);
        } else {
            boolean result = !((a == LogicState.HIGH) && (b == LogicState.HIGH) && (c == LogicState.HIGH));
            getPin(out).setState(result ? LogicState.HIGH : LogicState.LOW);
        }
    }

    @Override
    protected List<Pin> getOutputPins() {
        return List.of(getPin("12"), getPin("6"), getPin("8"));
    }

    @Override
    public List<String> getGateDescriptions() {
        return List.of(
                "Pin 14: Vcc    Pin 7: GND",
                "Gate 1: 1, 2, 13 → NAND → 12",
                "Gate 2: 3, 4, 5 → NAND → 6",
                "Gate 3: 9, 10, 11 → NAND → 8");
    }

    @Override
    public Component clone() {
        return new NANDGate7410();
    }
}
