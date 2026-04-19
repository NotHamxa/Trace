package com.trace.model.chips;

import com.trace.model.*;

import java.util.List;

public class NOTGate7404 extends ICChip {

    public NOTGate7404() {
        super("7404 NOT");
    }

    @Override
    protected PinType getDefaultPinType(int pinNumber) {
        // NOT gate: 6 inverters, outputs at pins 2, 4, 6, 8, 10, 12
        if (pinNumber == 2 || pinNumber == 4 || pinNumber == 6
                || pinNumber == 8 || pinNumber == 10 || pinNumber == 12) {
            return PinType.OUTPUT;
        }
        return PinType.INPUT;
    }

    @Override
    protected void computeGates() {
        applyInverter("1", "2");
        applyInverter("3", "4");
        applyInverter("5", "6");
        applyInverter("9", "8");
        applyInverter("11", "10");
        applyInverter("13", "12");
    }

    private void applyInverter(String in, String out) {
        LogicState a = getPin(in).getState();

        if (a == LogicState.FLOATING) {
            getPin(out).setState(LogicState.FLOATING);
        } else {
            getPin(out).setState(a == LogicState.HIGH ? LogicState.LOW : LogicState.HIGH);
        }
    }

    @Override
    protected List<Pin> getOutputPins() {
        return List.of(getPin("2"), getPin("4"), getPin("6"),
                getPin("8"), getPin("10"), getPin("12"));
    }

    @Override
    public List<String> getGateDescriptions() {
        return List.of(
                "Pin 14: Vcc    Pin 7: GND",
                "Gate 1: 1 → NOT → 2",
                "Gate 2: 3 → NOT → 4",
                "Gate 3: 5 → NOT → 6",
                "Gate 4: 9 → NOT → 8",
                "Gate 5: 11 → NOT → 10",
                "Gate 6: 13 → NOT → 12");
    }

    @Override
    public Component clone() {
        return new NOTGate7404();
    }
}
