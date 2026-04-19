package com.trace.model.chips;

import com.trace.model.*;

import java.util.List;

/**
 * 7483: 4-bit binary full adder with fast carry.
 *
 * 16-pin DIP pinout:
 *   1  A4      9  S1
 *   2  S3      10 A1
 *   3  A3      11 B1
 *   4  B3      12 GND
 *   5  VCC     13 C0 (carry in)
 *   6  S2      14 C4 (carry out)
 *   7  B2      15 B4
 *   8  A2      16 S4
 */
public class FourBitAdder7483 extends ICChip {

    public FourBitAdder7483() {
        super("7483 4-bit Adder", 16, 5, 12);
    }

    @Override
    protected PinType getDefaultPinType(int pinNumber) {
        // Sum outputs: 9 (S1), 6 (S2), 2 (S3), 16 (S4), and carry out 14 (C4)
        if (pinNumber == 9 || pinNumber == 6 || pinNumber == 2
                || pinNumber == 16 || pinNumber == 14) {
            return PinType.OUTPUT;
        }
        return PinType.INPUT;
    }

    @Override
    protected void computeGates() {
        int a = bit("A1", 0) | bit("A2", 1) | bit("A3", 2) | bit("A4", 3);
        int b = bit("B1", 0) | bit("B2", 1) | bit("B3", 2) | bit("B4", 3);
        int cin = (getPin("13").getState() == LogicState.HIGH) ? 1 : 0;

        if (hasFloatingInput()) {
            for (Pin p : getOutputPins()) p.setState(LogicState.FLOATING);
            return;
        }

        int sum = a + b + cin;
        setBit("S1", sum, 0);
        setBit("S2", sum, 1);
        setBit("S3", sum, 2);
        setBit("S4", sum, 3);
        getPin("14").setState(((sum >> 4) & 1) == 1 ? LogicState.HIGH : LogicState.LOW);
    }

    private int bit(String label, int shift) {
        Pin p = getPin(pinFor(label));
        return (p.getState() == LogicState.HIGH) ? (1 << shift) : 0;
    }

    private void setBit(String label, int value, int shift) {
        boolean hi = ((value >> shift) & 1) == 1;
        getPin(pinFor(label)).setState(hi ? LogicState.HIGH : LogicState.LOW);
    }

    private String pinFor(String label) {
        switch (label) {
            case "A1": return "10";
            case "A2": return "8";
            case "A3": return "3";
            case "A4": return "1";
            case "B1": return "11";
            case "B2": return "7";
            case "B3": return "4";
            case "B4": return "15";
            case "S1": return "9";
            case "S2": return "6";
            case "S3": return "2";
            case "S4": return "16";
        }
        throw new IllegalArgumentException(label);
    }

    private boolean hasFloatingInput() {
        String[] inputs = {"10", "8", "3", "1", "11", "7", "4", "15", "13"};
        for (String p : inputs) {
            if (getPin(p).getState() == LogicState.FLOATING) return true;
        }
        return false;
    }

    @Override
    protected List<Pin> getOutputPins() {
        return List.of(getPin("9"), getPin("6"), getPin("2"), getPin("16"), getPin("14"));
    }

    @Override
    public List<String> getGateDescriptions() {
        return List.of(
                "Pin 5: Vcc    Pin 12: GND",
                "A inputs: A0=10, A1=8, A2=3, A3=1",
                "B inputs: B0=11, B1=7, B2=4, B3=15",
                "Cin=13    Cout=14",
                "Sum outputs: S0=9, S1=6, S2=2, S3=16",
                "S[3:0] = A[3:0] + B[3:0] + Cin");
    }

    @Override
    public Component clone() {
        return new FourBitAdder7483();
    }
}
