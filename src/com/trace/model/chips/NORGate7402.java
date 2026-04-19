package com.trace.model.chips;

import com.trace.model.*;

import java.util.List;

public class NORGate7402 extends ICChip {

    public NORGate7402() {
        super("7402 NOR");
    }

    @Override
    protected PinType getDefaultPinType(int pinNumber) {
        // Real 7402: outputs at pins 1, 4, 10, 13
        if (pinNumber == 1 || pinNumber == 4 || pinNumber == 10 || pinNumber == 13) {
            return PinType.OUTPUT;
        }
        return PinType.INPUT;
    }

    @Override
    protected void computeGates() {
        // Real 7402 pinout: output first, then two inputs
        // Gate 1: Y=1, A=2, B=3
        // Gate 2: Y=4, A=5, B=6
        // Gate 3: Y=10, A=8, B=9
        // Gate 4: Y=13, A=11, B=12
        applyGate("2", "3", "1");
        applyGate("5", "6", "4");
        applyGate("8", "9", "10");
        applyGate("11", "12", "13");
    }

    private void applyGate(String inA, String inB, String out) {
        LogicState a = getPin(inA).getState();
        LogicState b = getPin(inB).getState();

        if (a == LogicState.FLOATING || b == LogicState.FLOATING) {
            getPin(out).setState(LogicState.FLOATING);
        } else {
            boolean result = !((a == LogicState.HIGH) || (b == LogicState.HIGH));
            getPin(out).setState(result ? LogicState.HIGH : LogicState.LOW);
        }
    }

    @Override
    protected List<Pin> getOutputPins() {
        return List.of(getPin("1"), getPin("4"), getPin("10"), getPin("13"));
    }

    @Override
    public List<String> getGateDescriptions() {
        return List.of(
                "Pin 14: Vcc    Pin 7: GND",
                "Gate 1: 2, 3 → NOR → 1",
                "Gate 2: 5, 6 → NOR → 4",
                "Gate 3: 8, 9 → NOR → 10",
                "Gate 4: 11, 12 → NOR → 13");
    }

    @Override
    public Component clone() {
        return new NORGate7402();
    }
}
