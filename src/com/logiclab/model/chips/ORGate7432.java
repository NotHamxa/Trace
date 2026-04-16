package com.logiclab.model.chips;

import com.logiclab.model.*;

import java.util.List;

public class ORGate7432 extends ICChip {

    public ORGate7432() {
        super("7432 OR");
    }

    @Override
    protected PinType getDefaultPinType(int pinNumber) {
        if (pinNumber == 3 || pinNumber == 6 || pinNumber == 8 || pinNumber == 11) {
            return PinType.OUTPUT;
        }
        return PinType.INPUT;
    }

    @Override
    protected void computeGates() {
        applyGate("1", "2", "3");
        applyGate("4", "5", "6");
        applyGate("9", "10", "8");
        applyGate("12", "13", "11");
    }

    private void applyGate(String inA, String inB, String out) {
        LogicState a = getPin(inA).getState();
        LogicState b = getPin(inB).getState();

        if (a == LogicState.FLOATING || b == LogicState.FLOATING) {
            getPin(out).setState(LogicState.FLOATING);
        } else {
            boolean result = (a == LogicState.HIGH) || (b == LogicState.HIGH);
            getPin(out).setState(result ? LogicState.HIGH : LogicState.LOW);
        }
    }

    @Override
    protected List<Pin> getOutputPins() {
        return List.of(getPin("3"), getPin("6"), getPin("8"), getPin("11"));
    }

    @Override
    public List<String> getGateDescriptions() {
        return List.of(
                "Pin 14: Vcc    Pin 7: GND",
                "Gate 1: 1, 2 → OR → 3",
                "Gate 2: 4, 5 → OR → 6",
                "Gate 3: 9, 10 → OR → 8",
                "Gate 4: 12, 13 → OR → 11");
    }

    @Override
    public Component clone() {
        return new ORGate7432();
    }
}
