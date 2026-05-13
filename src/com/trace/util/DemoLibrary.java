package com.trace.util;

import com.trace.model.Breadboard;
import com.trace.model.Circuit;
import com.trace.model.ICChip;
import com.trace.model.Wire;
import com.trace.model.chips.ANDGate7408;
import com.trace.model.chips.NANDGate7400;
import com.trace.model.chips.NANDGate7410;
import com.trace.model.chips.ORGate7432;
import com.trace.model.chips.XORGate7486;
import com.trace.model.input.DIPSwitch;
import com.trace.model.input.ToggleSwitch;
import com.trace.model.output.BinaryToBCDDisplay;
import com.trace.model.output.LED;
import com.trace.model.output.LightBar;
import com.trace.model.passive.Resistor;
import com.trace.model.power.Ground;
import com.trace.model.power.PowerSupply5V;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;

import com.trace.model.LogicState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DemoLibrary {

    public static class Demo {
        public final String name;
        public final String description;
        public final Supplier<Circuit> builder;
        public Demo(String name, String description, Supplier<Circuit> builder) {
            this.name = name;
            this.description = description;
            this.builder = builder;
        }
    }

    public static List<Demo> all() {
        List<Demo> demos = new ArrayList<>();
        demos.add(new Demo("Half Adder",
                "7486 XOR for SUM, 7408 AND for CARRY \u2014 two ICs, two LEDs.",
                DemoLibrary::buildHalfAdderDemo));
        demos.add(new Demo("Binary \u2192 Gray Code (4-bit)",
                "DIP switch drives a 4-bit binary value; a 7486 converts it to Gray code shown on a Light Bar.",
                DemoLibrary::buildBinaryToGrayDemo));
        demos.add(new Demo("Gray Code \u2192 Binary (4-bit)",
                "DIP switch drives a 4-bit Gray code; a 7486 decodes to binary, shown on a Light Bar and BCD display.",
                DemoLibrary::buildGrayToBinaryDemo));
        demos.add(new Demo("4-bit Ripple Adder",
                "Two 4-bit inputs + carry-in feed a full 4-bit ripple-carry adder built from 2\u00d77486, 2\u00d77408, 1\u00d77432 across two breadboards.",
                DemoLibrary::buildRippleAdderDemo));
        demos.add(new Demo("NAND Logic (4-bit)",
                "Four boolean functions (W,X,Y,Z) from inputs (A,B,C,D) built entirely from 7400 and 7410 NAND gates across two breadboards.",
                DemoLibrary::buildNandLogicDemo));
        return demos;
    }

    private static Circuit buildHalfAdderDemo() {
        Circuit c = new Circuit();
        c.setName("Demo: Half Adder");
        Breadboard bb = c.getBreadboard();
        ensurePositions(bb);

        int xorCol = 4;
        XORGate7486 xorIc = new XORGate7486();
        xorIc.setPosition(bb.getHoleX(xorCol) - 10, bb.getHoleY(4));
        attachIcSlots(c, bb, xorIc, xorCol);

        int andCol = 17;
        ANDGate7408 andIc = new ANDGate7408();
        andIc.setPosition(bb.getHoleX(andCol) - 10, bb.getHoleY(4));
        attachIcSlots(c, bb, andIc, andCol);

        double swX = bb.getHoleX(0) - 60;
        ToggleSwitch swA = new ToggleSwitch();
        swA.setPosition(swX, bb.getBoardY() + 340);
        ToggleSwitch swB = new ToggleSwitch();
        swB.setPosition(swX, bb.getBoardY() + 380);

        int colA = 2, colB = 3;

        Resistor r1 = new Resistor(220);
        r1.setPosition(bb.getHoleX(12), bb.getHoleY(5) - 6);
        LED sumLed = new LED(Color.LIME);
        sumLed.setPosition(bb.getHoleX(15) - 5, bb.getHoleY(5) + 25);

        Resistor r2 = new Resistor(220);
        r2.setPosition(bb.getHoleX(25), bb.getHoleY(5) - 6);
        LED carryLed = new LED(Color.RED);
        carryLed.setPosition(bb.getHoleX(28) - 5, bb.getHoleY(5) + 25);

        PowerSupply5V vcc = new PowerSupply5V();
        vcc.setPosition(bb.getHoleX(0) - 17, bb.getBoardY() - 40);
        Ground gnd = new Ground();
        gnd.setPosition(bb.getHoleX(29) - 17, bb.getBoardY() + 315);

        c.addComponent(xorIc);
        c.addComponent(andIc);
        c.addComponent(swA);
        c.addComponent(swB);
        c.addComponent(r1);
        c.addComponent(sumLed);
        c.addComponent(r2);
        c.addComponent(carryLed);
        c.addComponent(vcc);
        c.addComponent(gnd);

        c.addWire(new Wire(vcc.getPin("VCC"),
                bb.getTopPositive().getPoint(0).getOrCreateHolePin()));
        c.addWire(new Wire(gnd.getPin("GND"),
                bb.getBottomNegative().getPoint(29).getOrCreateHolePin()));

        wireIcPower(c, bb, xorCol);
        wireIcPower(c, bb, andCol);

        c.addWire(route(new Wire(swA.getPin("OUT"),
                bb.getHole(colA, 9).getOrCreateHolePin()),
                belowBoard(bb, 1)));
        c.addWire(route(new Wire(swB.getPin("OUT"),
                bb.getHole(colB, 9).getOrCreateHolePin()),
                belowBoard(bb, 3)));

        c.addWire(hop(bb, colA, xorCol, 6));
        c.addWire(hop(bb, xorCol, andCol, 6));
        c.addWire(hop(bb, colB, xorCol + 1, 7));
        c.addWire(hop(bb, xorCol + 1, andCol + 1, 7));

        c.addWire(new Wire(
                bb.getHole(xorCol + 2, 9).getOrCreateHolePin(), r1.getPin("1")));
        c.addWire(new Wire(r1.getPin("2"), sumLed.getPin("anode")));
        c.addWire(route(new Wire(sumLed.getPin("cathode"),
                bb.getBottomNegative().getPoint(16).getOrCreateHolePin()),
                belowBoard(bb, 0)));

        c.addWire(new Wire(
                bb.getHole(andCol + 2, 9).getOrCreateHolePin(), r2.getPin("1")));
        c.addWire(new Wire(r2.getPin("2"), carryLed.getPin("anode")));
        c.addWire(route(new Wire(carryLed.getPin("cathode"),
                bb.getBottomNegative().getPoint(29).getOrCreateHolePin()),
                belowBoard(bb, 2)));

        swA.setDisplayLabel("A");
        swB.setDisplayLabel("B");
        sumLed.setDisplayLabel("SUM");
        carryLed.setDisplayLabel("CARRY");

        setTests(c,
                new String[]{"00", "01", "10", "11"},
                new String[]{"00", "10", "10", "01"});  // SUM, CARRY

        c.setModified(false);
        return c;
    }

    private static Circuit buildBinaryToGrayDemo() {
        Circuit c = new Circuit();
        c.setName("Demo: Binary \u2192 Gray Code");
        Breadboard bb = c.getBreadboard();
        ensurePositions(bb);

        int xorCol = 10;
        XORGate7486 xorIc = new XORGate7486();
        xorIc.setPosition(bb.getHoleX(xorCol) - 10, bb.getHoleY(4));
        attachIcSlots(c, bb, xorIc, xorCol);

        int colB3 = 2, colB2 = 4, colB1 = 6, colB0 = 8;
        int colG3 = 19, colG2 = 21, colG1 = 23, colG0 = 25;

        DIPSwitch dip = new DIPSwitch(4);
        dip.setPosition(bb.getHoleX(colB3) - 16, bb.getBoardY() + 340);

        LightBar bar = new LightBar();
        bar.setPosition(bb.getHoleX(colG3) - 15, bb.getBoardY() + 340);

        PowerSupply5V vcc = new PowerSupply5V();
        vcc.setPosition(bb.getHoleX(0) - 17, bb.getBoardY() - 40);
        Ground gnd = new Ground();
        gnd.setPosition(bb.getHoleX(29) - 17, bb.getBoardY() + 395);

        c.addComponent(xorIc);
        c.addComponent(dip);
        c.addComponent(bar);
        c.addComponent(vcc);
        c.addComponent(gnd);

        c.addWire(new Wire(vcc.getPin("VCC"),
                bb.getTopPositive().getPoint(0).getOrCreateHolePin()));
        c.addWire(new Wire(gnd.getPin("GND"),
                bb.getBottomNegative().getPoint(29).getOrCreateHolePin()));

        wireIcPower(c, bb, xorCol);

        c.addWire(route(new Wire(dip.getPin("OUT1"),
                bb.getHole(colB3, 9).getOrCreateHolePin()),
                belowBoard(bb, 1)));
        c.addWire(route(new Wire(dip.getPin("OUT2"),
                bb.getHole(colB2, 9).getOrCreateHolePin()),
                belowBoard(bb, 3)));
        c.addWire(route(new Wire(dip.getPin("OUT3"),
                bb.getHole(colB1, 9).getOrCreateHolePin()),
                belowBoard(bb, 5)));
        c.addWire(route(new Wire(dip.getPin("OUT4"),
                bb.getHole(colB0, 9).getOrCreateHolePin()),
                belowBoard(bb, 7)));

        c.addWire(hop(bb, colB3, xorCol, 6));         // B3 → pin 1
        c.addWire(hop(bb, colB2, xorCol + 1, 7));     // B2 → pin 2
        c.addWire(hop(bb, xorCol + 2, colG2, 8));     // pin 3 → G2

        c.addWire(hop(bb, colB2, xorCol + 3, 6));     // B2 → pin 4 (chains with B2 above via col net)
        c.addWire(hop(bb, colB1, xorCol + 4, 7));     // B1 → pin 5
        c.addWire(hop(bb, xorCol + 5, colG1, 8));     // pin 6 → G1

        c.addWire(route(new Wire(
                bb.getHole(colB1, 9).getOrCreateHolePin(),
                bb.getHole(xorCol + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb, 0)));
        c.addWire(route(new Wire(
                bb.getHole(colB0, 9).getOrCreateHolePin(),
                bb.getHole(xorCol + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb, 1)));
        c.addWire(route(new Wire(
                bb.getHole(xorCol + 6, 0).getOrCreateHolePin(),
                bb.getHole(colG0, 9).getOrCreateHolePin()),
                aboveBoard(bb, 2)));

        c.addWire(hop(bb, colB3, colG3, 6));

        c.addWire(route(new Wire(bar.getPin("IN0"),
                bb.getHole(colG3, 9).getOrCreateHolePin()),
                belowBoard(bb, 10)));
        c.addWire(route(new Wire(bar.getPin("IN1"),
                bb.getHole(colG2, 9).getOrCreateHolePin()),
                belowBoard(bb, 12)));
        c.addWire(route(new Wire(bar.getPin("IN2"),
                bb.getHole(colG1, 9).getOrCreateHolePin()),
                belowBoard(bb, 13)));
        c.addWire(route(new Wire(bar.getPin("IN3"),
                bb.getHole(colG0, 9).getOrCreateHolePin()),
                belowBoard(bb, 14)));

        dip.setTag(0, "B3"); dip.setTag(1, "B2"); dip.setTag(2, "B1"); dip.setTag(3, "B0");
        bar.setTag(0, "G3"); bar.setTag(1, "G2"); bar.setTag(2, "G1"); bar.setTag(3, "G0");

        setTests(c, new String[]{
                "0000","0001","0010","0011","0100","0101","0110","0111",
                "1000","1001","1010","1011","1100","1101","1110","1111"
        }, new String[]{
                "0000DDDD","0001DDDD","0011DDDD","0010DDDD",
                "0110DDDD","0111DDDD","0101DDDD","0100DDDD",
                "1100DDDD","1101DDDD","1111DDDD","1110DDDD",
                "1010DDDD","1011DDDD","1001DDDD","1000DDDD"
        });

        c.setModified(false);
        return c;
    }

    private static Circuit buildGrayToBinaryDemo() {
        Circuit c = new Circuit();
        c.setName("Demo: Gray Code \u2192 Binary");
        Breadboard bb = c.getBreadboard();
        ensurePositions(bb);

        int xorCol = 10;
        XORGate7486 xorIc = new XORGate7486();
        xorIc.setPosition(bb.getHoleX(xorCol) - 10, bb.getHoleY(4));
        attachIcSlots(c, bb, xorIc, xorCol);

        int colG3 = 2, colG2 = 4, colG1 = 6, colG0 = 8;
        int colB3 = 19, colB2 = 21, colB1 = 23, colB0 = 25;

        DIPSwitch dip = new DIPSwitch(4);
        dip.setPosition(bb.getHoleX(colG3) - 16, bb.getBoardY() + 340);

        LightBar bar = new LightBar();
        bar.setPosition(bb.getHoleX(colB3) - 15, bb.getBoardY() + 340);

        BinaryToBCDDisplay bcd = new BinaryToBCDDisplay();
        bcd.setPosition(bb.getHoleX(colB3) - 15, bb.getBoardY() + 400);

        PowerSupply5V vcc = new PowerSupply5V();
        vcc.setPosition(bb.getHoleX(0) - 17, bb.getBoardY() - 40);
        Ground gnd = new Ground();
        gnd.setPosition(bb.getHoleX(29) - 17, bb.getBoardY() + 395);

        c.addComponent(xorIc);
        c.addComponent(dip);
        c.addComponent(bar);
        c.addComponent(bcd);
        c.addComponent(vcc);
        c.addComponent(gnd);

        c.addWire(new Wire(vcc.getPin("VCC"),
                bb.getTopPositive().getPoint(0).getOrCreateHolePin()));
        c.addWire(new Wire(gnd.getPin("GND"),
                bb.getBottomNegative().getPoint(29).getOrCreateHolePin()));

        wireIcPower(c, bb, xorCol);

        c.addWire(route(new Wire(dip.getPin("OUT1"),
                bb.getHole(colG3, 9).getOrCreateHolePin()),
                belowBoard(bb, 1)));
        c.addWire(route(new Wire(dip.getPin("OUT2"),
                bb.getHole(colG2, 9).getOrCreateHolePin()),
                belowBoard(bb, 3)));
        c.addWire(route(new Wire(dip.getPin("OUT3"),
                bb.getHole(colG1, 9).getOrCreateHolePin()),
                belowBoard(bb, 5)));
        c.addWire(route(new Wire(dip.getPin("OUT4"),
                bb.getHole(colG0, 9).getOrCreateHolePin()),
                belowBoard(bb, 7)));

        c.addWire(hop(bb, colG3, colB3, 6));

        c.addWire(hop(bb, colG2, xorCol, 7));         // G2 → pin 1
        c.addWire(hop(bb, colG3, xorCol + 1, 8));     // G3 → pin 2
        c.addWire(hop(bb, xorCol + 2, colB2, 6));     // pin 3 → B2

        c.addWire(hop(bb, colG1, xorCol + 3, 7));     // G1 → pin 4
        c.addWire(hop(bb, colB2, xorCol + 4, 8));     // B2 → pin 5
        c.addWire(hop(bb, xorCol + 5, colB1, 6));     // pin 6 → B1

        c.addWire(route(new Wire(
                bb.getHole(colG0, 9).getOrCreateHolePin(),
                bb.getHole(xorCol + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb, 0)));
        c.addWire(route(new Wire(
                bb.getHole(colB1, 9).getOrCreateHolePin(),
                bb.getHole(xorCol + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb, 1)));
        c.addWire(route(new Wire(
                bb.getHole(xorCol + 6, 0).getOrCreateHolePin(),
                bb.getHole(colB0, 9).getOrCreateHolePin()),
                aboveBoard(bb, 2)));

        c.addWire(route(new Wire(bar.getPin("IN0"),
                bb.getHole(colB3, 9).getOrCreateHolePin()),
                belowBoard(bb, 8)));
        c.addWire(route(new Wire(bar.getPin("IN1"),
                bb.getHole(colB2, 9).getOrCreateHolePin()),
                belowBoard(bb, 10)));
        c.addWire(route(new Wire(bar.getPin("IN2"),
                bb.getHole(colB1, 9).getOrCreateHolePin()),
                belowBoard(bb, 12)));
        c.addWire(route(new Wire(bar.getPin("IN3"),
                bb.getHole(colB0, 9).getOrCreateHolePin()),
                belowBoard(bb, 14)));

        c.addWire(route(new Wire(bcd.getPin("B3"),
                bb.getHole(colB3, 9).getOrCreateHolePin()),
                belowBoard(bb, 15)));
        c.addWire(route(new Wire(bcd.getPin("B2"),
                bb.getHole(colB2, 9).getOrCreateHolePin()),
                belowBoard(bb, 16)));
        c.addWire(route(new Wire(bcd.getPin("B1"),
                bb.getHole(colB1, 9).getOrCreateHolePin()),
                belowBoard(bb, 17)));
        c.addWire(route(new Wire(bcd.getPin("B0"),
                bb.getHole(colB0, 9).getOrCreateHolePin()),
                belowBoard(bb, 18)));

        dip.setTag(0, "G3"); dip.setTag(1, "G2"); dip.setTag(2, "G1"); dip.setTag(3, "G0");
        bar.setTag(0, "B3"); bar.setTag(1, "B2"); bar.setTag(2, "B1"); bar.setTag(3, "B0");

        setTests(c, new String[]{
                "0000","0001","0010","0011","0100","0101","0110","0111",
                "1000","1001","1010","1011","1100","1101","1110","1111"
        }, new String[]{
                "0000DDDD","0001DDDD","0011DDDD","0010DDDD",
                "0111DDDD","0110DDDD","0100DDDD","0101DDDD",
                "1111DDDD","1110DDDD","1100DDDD","1101DDDD",
                "1000DDDD","1001DDDD","1011DDDD","1010DDDD"
        });

        c.setModified(false);
        return c;
    }

    private static Circuit buildRippleAdderDemo() {
        Circuit c = new Circuit();
        c.setName("Demo: 4-bit Ripple Adder");

        Breadboard bb1 = c.getBreadboard();
        ensurePositions(bb1);

        Breadboard bb2 = new Breadboard(30);
        bb2.setPosition(bb1.getBoardX() + bb1.getBoardWidth() + 40, bb1.getBoardY());
        c.addBreadboard(bb2);
        ensurePositions(bb2);

        int xor1Col = 4, xor2Col = 13, and1Col = 22;
        int and2Col = 4, orCol = 13;

        XORGate7486 xor1 = new XORGate7486();
        xor1.setPosition(bb1.getHoleX(xor1Col) - 10, bb1.getHoleY(4));
        attachIcSlots(c, bb1, xor1, xor1Col);
        c.addComponent(xor1);

        XORGate7486 xor2 = new XORGate7486();
        xor2.setPosition(bb1.getHoleX(xor2Col) - 10, bb1.getHoleY(4));
        attachIcSlots(c, bb1, xor2, xor2Col);
        c.addComponent(xor2);

        ANDGate7408 and1 = new ANDGate7408();
        and1.setPosition(bb1.getHoleX(and1Col) - 10, bb1.getHoleY(4));
        attachIcSlots(c, bb1, and1, and1Col);
        c.addComponent(and1);

        ANDGate7408 and2 = new ANDGate7408();
        and2.setPosition(bb2.getHoleX(and2Col) - 10, bb2.getHoleY(4));
        attachIcSlots(c, bb2, and2, and2Col);
        c.addComponent(and2);

        ORGate7432 orIc = new ORGate7432();
        orIc.setPosition(bb2.getHoleX(orCol) - 10, bb2.getHoleY(4));
        attachIcSlots(c, bb2, orIc, orCol);
        c.addComponent(orIc);

        PowerSupply5V vcc = new PowerSupply5V();
        vcc.setPosition(bb1.getHoleX(0) - 17, bb1.getBoardY() - 40);
        Ground gnd = new Ground();
        gnd.setPosition(bb2.getHoleX(29) - 17, bb2.getBoardY() + 395);
        c.addComponent(vcc);
        c.addComponent(gnd);

        c.addWire(new Wire(vcc.getPin("VCC"),
                bb1.getTopPositive().getPoint(0).getOrCreateHolePin()));
        c.addWire(new Wire(gnd.getPin("GND"),
                bb2.getBottomNegative().getPoint(29).getOrCreateHolePin()));

        c.addWire(route(new Wire(
                bb1.getTopPositive().getPoint(29).getOrCreateHolePin(),
                bb2.getTopPositive().getPoint(0).getOrCreateHolePin()),
                aboveBoard(bb1, 0)));
        c.addWire(route(new Wire(
                bb1.getBottomNegative().getPoint(29).getOrCreateHolePin(),
                bb2.getBottomNegative().getPoint(0).getOrCreateHolePin()),
                belowBoard(bb1, 0)));

        wireIcPower(c, bb1, xor1Col);
        wireIcPower(c, bb1, xor2Col);
        wireIcPower(c, bb1, and1Col);
        wireIcPower(c, bb2, and2Col);
        wireIcPower(c, bb2, orCol);

        int cA3 = 0, cA2 = 1, cA1 = 2, cA0 = 3;        // A bus (left of XOR1)
        int cB3 = 12, cB2 = 11, cB1 = 20, cB0 = 21;    // B bus spread across bb1 free cols
        int cCin = 29;                                  // Carry-in bus on bb1 right

        DIPSwitch dipA = new DIPSwitch(4);
        dipA.setPosition(bb1.getHoleX(0) - 5, bb1.getBoardY() + 340);
        c.addComponent(dipA);

        DIPSwitch dipB = new DIPSwitch(4);
        dipB.setPosition(bb1.getHoleX(10) - 5, bb1.getBoardY() + 340);
        c.addComponent(dipB);

        ToggleSwitch swCin = new ToggleSwitch();
        swCin.setPosition(bb1.getHoleX(29) - 50, bb1.getBoardY() + 340);
        c.addComponent(swCin);

        c.addWire(route(new Wire(dipA.getPin("OUT1"), bb1.getHole(cA3, 9).getOrCreateHolePin()),
                belowBoard(bb1, 2)));
        c.addWire(route(new Wire(dipA.getPin("OUT2"), bb1.getHole(cA2, 9).getOrCreateHolePin()),
                belowBoard(bb1, 4)));
        c.addWire(route(new Wire(dipA.getPin("OUT3"), bb1.getHole(cA1, 9).getOrCreateHolePin()),
                belowBoard(bb1, 6)));
        c.addWire(route(new Wire(dipA.getPin("OUT4"), bb1.getHole(cA0, 9).getOrCreateHolePin()),
                belowBoard(bb1, 8)));

        c.addWire(route(new Wire(dipB.getPin("OUT1"), bb1.getHole(cB3, 9).getOrCreateHolePin()),
                belowBoard(bb1, 10)));
        c.addWire(route(new Wire(dipB.getPin("OUT2"), bb1.getHole(cB2, 9).getOrCreateHolePin()),
                belowBoard(bb1, 12)));
        c.addWire(route(new Wire(dipB.getPin("OUT3"), bb1.getHole(cB1, 9).getOrCreateHolePin()),
                belowBoard(bb1, 14)));
        c.addWire(route(new Wire(dipB.getPin("OUT4"), bb1.getHole(cB0, 9).getOrCreateHolePin()),
                belowBoard(bb1, 16)));

        c.addWire(route(new Wire(swCin.getPin("OUT"), bb1.getHole(cCin, 9).getOrCreateHolePin()),
                belowBoard(bb1, 18)));

        LightBar bar = new LightBar();
        bar.setPosition(bb2.getHoleX(6) - 15, bb2.getBoardY() + 340);
        c.addComponent(bar);

        c.addWire(hop(bb1, cA0, xor1Col, 6));
        c.addWire(hop(bb1, cB0, xor1Col + 1, 6));

        c.addWire(hop(bb1, xor1Col + 2, xor1Col + 3, 6));
        c.addWire(hop(bb1, cCin, xor1Col + 4, 6));

        c.addWire(route(new Wire(
                bb1.getHole(cA1, 9).getOrCreateHolePin(),
                bb1.getHole(xor1Col + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 2)));
        c.addWire(route(new Wire(
                bb1.getHole(cB1, 9).getOrCreateHolePin(),
                bb1.getHole(xor1Col + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 4)));

        c.addWire(hop(bb1, xor1Col + 6, xor1Col + 2, 3));

        c.addWire(hop(bb1, cA2, xor2Col, 6));
        c.addWire(hop(bb1, cB2, xor2Col + 1, 6));

        c.addWire(hop(bb1, xor2Col + 2, xor2Col + 3, 6));

        c.addWire(route(new Wire(
                bb1.getHole(cA3, 9).getOrCreateHolePin(),
                bb1.getHole(xor2Col + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 8)));
        c.addWire(route(new Wire(
                bb1.getHole(cB3, 9).getOrCreateHolePin(),
                bb1.getHole(xor2Col + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 10)));

        c.addWire(hop(bb1, xor2Col + 6, xor2Col + 2, 3));

        c.addWire(hop(bb1, cA0, and1Col, 7));
        c.addWire(hop(bb1, cB0, and1Col + 1, 7));

        c.addWire(hop(bb1, xor1Col + 2, and1Col + 3, 7));
        c.addWire(hop(bb1, cCin, and1Col + 4, 7));

        c.addWire(route(new Wire(
                bb1.getHole(cA1, 9).getOrCreateHolePin(),
                bb1.getHole(and1Col + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 14)));
        c.addWire(route(new Wire(
                bb1.getHole(cB1, 9).getOrCreateHolePin(),
                bb1.getHole(and1Col + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 16)));

        c.addWire(hop(bb1, xor1Col + 6, and1Col + 2, 3));

        c.addWire(route(new Wire(
                bb1.getHole(cA2, 9).getOrCreateHolePin(),
                bb2.getHole(and2Col, 9).getOrCreateHolePin()),
                belowBoard(bb1, 42)));
        c.addWire(route(new Wire(
                bb1.getHole(cB2, 9).getOrCreateHolePin(),
                bb2.getHole(and2Col + 1, 9).getOrCreateHolePin()),
                belowBoard(bb1, 44)));

        c.addWire(route(new Wire(
                bb1.getHole(xor2Col + 2, 9).getOrCreateHolePin(),
                bb2.getHole(and2Col + 3, 9).getOrCreateHolePin()),
                belowBoard(bb1, 46)));

        c.addWire(route(new Wire(
                bb1.getHole(cA3, 9).getOrCreateHolePin(),
                bb2.getHole(and2Col + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 20)));
        c.addWire(route(new Wire(
                bb1.getHole(cB3, 9).getOrCreateHolePin(),
                bb2.getHole(and2Col + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 22)));

        c.addWire(route(new Wire(
                bb1.getHole(xor2Col + 6, 0).getOrCreateHolePin(),
                bb2.getHole(and2Col + 2, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 24)));

        c.addWire(route(new Wire(
                bb1.getHole(and1Col + 2, 9).getOrCreateHolePin(),
                bb2.getHole(orCol, 9).getOrCreateHolePin()),
                belowBoard(bb1, 48)));
        c.addWire(route(new Wire(
                bb1.getHole(and1Col + 5, 9).getOrCreateHolePin(),
                bb2.getHole(orCol + 1, 9).getOrCreateHolePin()),
                belowBoard(bb1, 50)));
        c.addWire(route(new Wire(
                bb2.getHole(orCol + 2, 9).getOrCreateHolePin(),
                bb1.getHole(xor1Col + 1, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 26)));
        c.addWire(route(new Wire(
                bb2.getHole(orCol + 2, 9).getOrCreateHolePin(),
                bb1.getHole(and1Col + 1, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 28)));

        c.addWire(route(new Wire(
                bb1.getHole(and1Col + 6, 0).getOrCreateHolePin(),
                bb2.getHole(orCol + 3, 9).getOrCreateHolePin()),
                aboveBoard(bb1, 30)));
        c.addWire(route(new Wire(
                bb1.getHole(and1Col + 3, 0).getOrCreateHolePin(),
                bb2.getHole(orCol + 4, 9).getOrCreateHolePin()),
                aboveBoard(bb1, 32)));
        c.addWire(route(new Wire(
                bb2.getHole(orCol + 5, 9).getOrCreateHolePin(),
                bb1.getHole(xor2Col + 4, 9).getOrCreateHolePin()),
                belowBoard(bb1, 52)));
        c.addWire(hop(bb2, orCol + 5, and2Col + 4, 6));

        c.addWire(route(new Wire(
                bb2.getHole(and2Col + 2, 9).getOrCreateHolePin(),
                bb2.getHole(orCol + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb2, 0)));
        c.addWire(route(new Wire(
                bb2.getHole(and2Col + 5, 9).getOrCreateHolePin(),
                bb2.getHole(orCol + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb2, 2)));
        c.addWire(route(new Wire(
                bb2.getHole(orCol + 6, 0).getOrCreateHolePin(),
                bb1.getHole(xor2Col + 1, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 34)));
        c.addWire(hop(bb2, orCol + 6, and2Col + 1, 3));

        c.addWire(hop(bb2, and2Col + 6, orCol + 2, 3));
        c.addWire(hop(bb2, and2Col + 3, orCol + 1, 2));

        c.addWire(route(new Wire(bar.getPin("IN0"),
                bb2.getHole(orCol + 3, 0).getOrCreateHolePin()),
                aboveBoard(bb2, 10)));
        c.addWire(route(new Wire(bar.getPin("IN1"),
                bb1.getHole(xor2Col + 3, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 36)));
        c.addWire(route(new Wire(bar.getPin("IN2"),
                bb1.getHole(xor2Col + 5, 9).getOrCreateHolePin()),
                belowBoard(bb2, 3)));
        c.addWire(route(new Wire(bar.getPin("IN3"),
                bb1.getHole(xor1Col + 3, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 38)));
        c.addWire(route(new Wire(bar.getPin("IN4"),
                bb1.getHole(xor1Col + 5, 9).getOrCreateHolePin()),
                belowBoard(bb2, 5)));

        dipA.setTag(0, "A3"); dipA.setTag(1, "A2"); dipA.setTag(2, "A1"); dipA.setTag(3, "A0");
        dipB.setTag(0, "B3"); dipB.setTag(1, "B2"); dipB.setTag(2, "B1"); dipB.setTag(3, "B0");
        swCin.setDisplayLabel("Cin");
        bar.setTag(0, "Cout"); bar.setTag(1, "S3"); bar.setTag(2, "S2"); bar.setTag(3, "S1"); bar.setTag(4, "S0");

        setTests(c, new String[]{
                "000000000",  // 0+0+0 = 0
                "000000001",  // 0+0+1 = 1
                "000100010",  // 1+1+0 = 2
                "000100011",  // 1+1+1 = 3
                "001000100",  // 2+2+0 = 4
                "010101010",  // 5+5+0 = 10
                "011101110",  // 7+7+0 = 14
                "011101111",  // 7+7+1 = 15
                "111100001",  // 15+0+1 = 16
                "111101111",  // 15+7+1 = 23
                "111111110",  // 15+15+0 = 30
                "111111111",  // 15+15+1 = 31
                "100000000",  // 8+0+0 = 8
                "000010000",  // 0+8+0 = 8
                "100010000",  // 8+8+0 = 16
                "100110011"   // 9+9+1 = 19
        }, new String[]{
                "00000DDD",   // 0
                "00001DDD",   // 1
                "00010DDD",   // 2
                "00011DDD",   // 3
                "00100DDD",   // 4
                "01010DDD",   // 10
                "01110DDD",   // 14
                "01111DDD",   // 15
                "10000DDD",   // 16
                "10111DDD",   // 23
                "11110DDD",   // 30
                "11111DDD",   // 31
                "01000DDD",   // 8
                "01000DDD",   // 8
                "10000DDD",   // 16
                "10011DDD"    // 19
        });

        c.setModified(false);
        return c;
    }

    private static Circuit buildNandLogicDemo() {
        Circuit c = new Circuit();
        c.setName("Demo: NAND Logic");

        Breadboard bb1 = c.getBreadboard();
        ensurePositions(bb1);

        Breadboard bb2 = new Breadboard(30);
        bb2.setPosition(bb1.getBoardX() + bb1.getBoardWidth() + 40, bb1.getBoardY());
        c.addBreadboard(bb2);
        ensurePositions(bb2);

        int n1Col = 7, n2Col = 17;          // bb1
        int n3Col = 4, n3iCol = 15;         // bb2

        NANDGate7400 nand1 = new NANDGate7400();
        nand1.setPosition(bb1.getHoleX(n1Col) - 10, bb1.getHoleY(4));
        attachIcSlots(c, bb1, nand1, n1Col);
        c.addComponent(nand1);

        NANDGate7400 nand2 = new NANDGate7400();
        nand2.setPosition(bb1.getHoleX(n2Col) - 10, bb1.getHoleY(4));
        attachIcSlots(c, bb1, nand2, n2Col);
        c.addComponent(nand2);

        NANDGate7400 nand3 = new NANDGate7400();
        nand3.setPosition(bb2.getHoleX(n3Col) - 10, bb2.getHoleY(4));
        attachIcSlots(c, bb2, nand3, n3Col);
        c.addComponent(nand3);

        NANDGate7410 nand3i = new NANDGate7410();
        nand3i.setPosition(bb2.getHoleX(n3iCol) - 10, bb2.getHoleY(4));
        attachIcSlots(c, bb2, nand3i, n3iCol);
        c.addComponent(nand3i);

        PowerSupply5V vcc = new PowerSupply5V();
        vcc.setPosition(bb1.getHoleX(0) - 17, bb1.getBoardY() - 40);
        Ground gnd = new Ground();
        gnd.setPosition(bb2.getHoleX(29) - 17, bb2.getBoardY() + 395);
        c.addComponent(vcc);
        c.addComponent(gnd);

        c.addWire(new Wire(vcc.getPin("VCC"),
                bb1.getTopPositive().getPoint(0).getOrCreateHolePin()));
        c.addWire(new Wire(gnd.getPin("GND"),
                bb2.getBottomNegative().getPoint(29).getOrCreateHolePin()));

        c.addWire(route(new Wire(
                bb1.getTopPositive().getPoint(29).getOrCreateHolePin(),
                bb2.getTopPositive().getPoint(0).getOrCreateHolePin()),
                aboveBoard(bb1, 0)));
        c.addWire(route(new Wire(
                bb1.getBottomNegative().getPoint(29).getOrCreateHolePin(),
                bb2.getBottomNegative().getPoint(0).getOrCreateHolePin()),
                belowBoard(bb1, 0)));

        wireIcPower(c, bb1, n1Col);
        wireIcPower(c, bb1, n2Col);
        wireIcPower(c, bb2, n3Col);
        wireIcPower(c, bb2, n3iCol);

        int cA = 0, cB = 1, cC = 2, cD = 3;

        DIPSwitch dip = new DIPSwitch(4);
        dip.setPosition(bb1.getHoleX(0) - 5, bb1.getBoardY() + 340);
        c.addComponent(dip);

        c.addWire(route(new Wire(dip.getPin("OUT1"), bb1.getHole(cA, 9).getOrCreateHolePin()),
                belowBoard(bb1, 2)));
        c.addWire(route(new Wire(dip.getPin("OUT2"), bb1.getHole(cB, 9).getOrCreateHolePin()),
                belowBoard(bb1, 4)));
        c.addWire(route(new Wire(dip.getPin("OUT3"), bb1.getHole(cC, 9).getOrCreateHolePin()),
                belowBoard(bb1, 6)));
        c.addWire(route(new Wire(dip.getPin("OUT4"), bb1.getHole(cD, 9).getOrCreateHolePin()),
                belowBoard(bb1, 8)));

        LightBar bar = new LightBar();
        bar.setPosition(bb2.getHoleX(22) - 15, bb2.getBoardY() + 340);
        c.addComponent(bar);

        int colW = 22, colX = 24, colY = 26, colZ = 28;

        c.addWire(hop(bb1, cC, n1Col, 6));         // C → col 7 (row g)
        c.addWire(hop(bb1, n1Col, n1Col + 1, 7));  // col 7 → col 8 (row h, ties both inputs)

        c.addWire(hop(bb1, cA, n1Col + 3, 8));     // A → col 10 (row i)
        c.addWire(hop(bb1, n1Col + 3, n1Col + 4, 7)); // col 10 → col 11 (row h)

        c.addWire(route(new Wire(
                bb1.getHole(cB, 9).getOrCreateHolePin(),
                bb1.getHole(n1Col + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 2)));
        c.addWire(hop(bb1, n1Col + 5, n1Col + 4, 3));  // col 12 → col 11 (row d)

        c.addWire(route(new Wire(
                bb1.getHole(cD, 9).getOrCreateHolePin(),
                bb1.getHole(n1Col + 2, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 4)));
        c.addWire(hop(bb1, n1Col + 2, n1Col + 1, 2));  // col 9 → col 8 (row c)

        c.addWire(hop(bb1, cC, n2Col, 6));          // C → col 17 (row g)
        c.addWire(hop(bb1, cD, n2Col + 1, 8));      // D → col 18 (row i)

        c.addWire(hop(bb1, n1Col + 5, n2Col + 3, 6)); // A'(col 12) → col 20 (row g)
        c.addWire(hop(bb1, n2Col + 2, n2Col + 4, 7)); // (CD)'(col 19) → col 21 (row h)

        c.addWire(route(new Wire(
                bb1.getHole(cA, 9).getOrCreateHolePin(),
                bb1.getHole(n2Col + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 6)));
        c.addWire(route(new Wire(
                bb1.getHole(cB, 9).getOrCreateHolePin(),
                bb1.getHole(n2Col + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 8)));

        c.addWire(hop(bb1, n2Col + 6, n2Col + 2, 3)); // (AB)' col 23 → col 19 (row d)

        c.addWire(route(new Wire(
                bb1.getHole(n1Col + 5, 9).getOrCreateHolePin(),
                bb2.getHole(n3Col, 9).getOrCreateHolePin()),
                belowBoard(bb1, 10)));
        c.addWire(route(new Wire(
                bb1.getHole(cC, 9).getOrCreateHolePin(),
                bb2.getHole(n3Col + 1, 9).getOrCreateHolePin()),
                belowBoard(bb1, 12)));

        c.addWire(route(new Wire(
                bb1.getHole(cB, 9).getOrCreateHolePin(),
                bb2.getHole(n3Col + 3, 9).getOrCreateHolePin()),
                belowBoard(bb1, 14)));
        c.addWire(route(new Wire(
                bb1.getHole(cD, 9).getOrCreateHolePin(),
                bb2.getHole(n3Col + 4, 9).getOrCreateHolePin()),
                belowBoard(bb1, 16)));

        c.addWire(route(new Wire(
                bb1.getHole(cA, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol, 9).getOrCreateHolePin()),
                belowBoard(bb1, 18)));
        c.addWire(route(new Wire(
                bb1.getHole(cC, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 1, 9).getOrCreateHolePin()),
                belowBoard(bb1, 20)));
        c.addWire(route(new Wire(
                bb1.getHole(cD, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 1, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 10)));

        c.addWire(route(new Wire(
                bb1.getHole(n1Col + 6, 0).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 2, 9).getOrCreateHolePin()),
                aboveBoard(bb1, 12)));
        c.addWire(route(new Wire(
                bb1.getHole(n1Col + 2, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 3, 9).getOrCreateHolePin()),
                belowBoard(bb1, 22)));
        c.addWire(route(new Wire(
                bb1.getHole(n1Col + 3, 0).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 4, 9).getOrCreateHolePin()),
                aboveBoard(bb1, 14)));

        c.addWire(route(new Wire(
                bb2.getHole(n3Col + 2, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 3, 0).getOrCreateHolePin()),
                aboveBoard(bb2, 0)));
        c.addWire(route(new Wire(
                bb2.getHole(n3Col + 5, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb2, 2)));
        c.addWire(route(new Wire(
                bb2.getHole(n3iCol + 5, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb2, 4)));

        c.addWire(route(new Wire(
                bb2.getHole(n3iCol + 2, 0).getOrCreateHolePin(),
                bb1.getHole(n2Col + 1, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 16)));

        c.addWire(route(new Wire(
                bb1.getHole(n2Col + 3, 0).getOrCreateHolePin(),
                bb2.getHole(colW, 9).getOrCreateHolePin()),
                aboveBoard(bb1, 18)));

        c.addWire(route(new Wire(
                bb1.getHole(n2Col + 5, 9).getOrCreateHolePin(),
                bb2.getHole(colX, 9).getOrCreateHolePin()),
                belowBoard(bb1, 24)));

        c.addWire(route(new Wire(
                bb2.getHole(n3iCol + 6, 0).getOrCreateHolePin(),
                bb2.getHole(colY, 9).getOrCreateHolePin()),
                aboveBoard(bb2, 6)));

        c.addWire(route(new Wire(
                bb1.getHole(n1Col + 2, 9).getOrCreateHolePin(),
                bb2.getHole(colZ, 9).getOrCreateHolePin()),
                belowBoard(bb1, 26)));

        c.addWire(route(new Wire(bar.getPin("IN0"),
                bb2.getHole(colW, 9).getOrCreateHolePin()),
                belowBoard(bb2, 8)));
        c.addWire(route(new Wire(bar.getPin("IN1"),
                bb2.getHole(colX, 9).getOrCreateHolePin()),
                belowBoard(bb2, 10)));
        c.addWire(route(new Wire(bar.getPin("IN2"),
                bb2.getHole(colY, 9).getOrCreateHolePin()),
                belowBoard(bb2, 12)));
        c.addWire(route(new Wire(bar.getPin("IN3"),
                bb2.getHole(colZ, 9).getOrCreateHolePin()),
                belowBoard(bb2, 14)));

        dip.setTag(0, "A"); dip.setTag(1, "B"); dip.setTag(2, "C"); dip.setTag(3, "D");
        bar.setTag(0, "W"); bar.setTag(1, "X"); bar.setTag(2, "Y"); bar.setTag(3, "Z");

        setTests(c, new String[]{
                "0000","0001","0010","0011","0100","0101","0110","0111",
                "1000","1001","1010","1011","1100","1101","1110","1111"
        }, new String[]{
                "0011DDDD",  // 0000: W=0 X=0 Y=1 Z=1
                "0001DDDD",  // 0001: W=0 X=0 Y=0 Z=1
                "0010DDDD",  // 0010: W=0 X=0 Y=1 Z=0
                "0110DDDD",  // 0011: W=0 X=1 Y=1 Z=0
                "0001DDDD",  // 0100: W=0 X=0 Y=0 Z=1
                "0011DDDD",  // 0101: W=0 X=0 Y=1 Z=1
                "0010DDDD",  // 0110: W=0 X=0 Y=1 Z=0
                "0110DDDD",  // 0111: W=0 X=1 Y=1 Z=0
                "0111DDDD",  // 1000: W=0 X=1 Y=1 Z=1
                "0101DDDD",  // 1001: W=0 X=1 Y=0 Z=1
                "0100DDDD",  // 1010: W=0 X=1 Y=0 Z=0
                "1100DDDD",  // 1011: W=1 X=1 Y=0 Z=0
                "1101DDDD",  // 1100: W=1 X=1 Y=0 Z=1
                "1111DDDD",  // 1101: W=1 X=1 Y=1 Z=1
                "1100DDDD",  // 1110: W=1 X=1 Y=0 Z=0
                "1110DDDD"   // 1111: W=1 X=1 Y=1 Z=0
        });

        c.setModified(false);
        return c;
    }

    private static void attachIcSlots(Circuit c, Breadboard bb, ICChip ic, int icCol) {
        for (int i = 0; i < 7; i++) {
            c.addWire(new Wire(
                    ic.getPin(String.valueOf(i + 1)),
                    bb.getHole(icCol + i, 5).getOrCreateHolePin()));
            c.addWire(new Wire(
                    ic.getPin(String.valueOf(14 - i)),
                    bb.getHole(icCol + i, 4).getOrCreateHolePin()));
        }
    }

    private static void wireIcPower(Circuit c, Breadboard bb, int icCol) {
        c.addWire(new Wire(
                bb.getTopPositive().getPoint(icCol).getOrCreateHolePin(),
                bb.getHole(icCol, 0).getOrCreateHolePin()));
        c.addWire(new Wire(
                bb.getBottomNegative().getPoint(icCol + 6).getOrCreateHolePin(),
                bb.getHole(icCol + 6, 9).getOrCreateHolePin()));
    }

    private static void ensurePositions(Breadboard bb) {
        bb.render(new Canvas(1, 1).getGraphicsContext2D());
    }

    private static Wire route(Wire w, double laneY) {
        double sx = w.getStartPin().getX();
        double ex = w.getEndPin().getX();
        w.getWaypoints().add(new double[]{sx, laneY});
        w.getWaypoints().add(new double[]{ex, laneY});
        return w;
    }

    private static double belowBoard(Breadboard bb, int idx) {
        return bb.getBoardY() + 308 + idx * 5;
    }

    private static double aboveBoard(Breadboard bb, int idx) {
        return bb.getBoardY() - 8 - idx * 5;
    }

    private static Wire hop(Breadboard bb, int colA, int colB, int letter) {
        return new Wire(
                bb.getHole(colA, letter).getOrCreateHolePin(),
                bb.getHole(colB, letter).getOrCreateHolePin());
    }

    private static LogicState[] bits(String s) {
        LogicState[] arr = new LogicState[s.length()];
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '1') arr[i] = LogicState.HIGH;
            else if (ch == '0') arr[i] = LogicState.LOW;
            else arr[i] = null; // don't-care
        }
        return arr;
    }

    private static void setTests(Circuit c, String[] inputRows, String[] expectedRows) {
        List<LogicState[]> inputs = new ArrayList<>();
        List<LogicState[]> expected = new ArrayList<>();
        for (int i = 0; i < inputRows.length; i++) {
            inputs.add(bits(inputRows[i]));
            expected.add(bits(expectedRows[i]));
        }
        c.setSavedTests(inputs, expected);
    }
}
