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

/**
 * Pre-built sample circuits used by the Start menu "Demos" panel.
 * Each demo returns a fresh in-memory Circuit that the host can open
 * directly as if it were a loaded .trc file.
 *
 * Conventions used throughout the demos:
 *  - The top + rail carries 5V, the bottom − rail carries GND. Bottom +
 *    and top − rails are left unused to keep power runs short.
 *  - IC VCC (pin 14) reaches the top + rail through a short stalk in its
 *    own column on the 'a' row. IC GND (pin 7) reaches the bottom − rail
 *    through a short stalk in its own column on the 'j' row.
 *  - Signals driven from below the board enter the chip's f-j net via
 *    letter 'j' (row 9); signals crossing the chip top enter via letter
 *    'a' (row 0). This lets each wire be routed orthogonally around the
 *    chip rather than through it.
 *  - Multi-bit buses follow the project-wide convention: the leftmost
 *    pin/input on an array component is the MSB, the rightmost is the LSB.
 *    So DIPSwitch.OUT1 drives the most significant bit, LightBar.IN0
 *    shows the most significant bit, etc.
 */
public class DemoLibrary {

    /** One entry on the Demos panel. */
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

    // ====================================================================
    // Individual demo builders
    // ====================================================================

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

        // Two stacked input switches, left of the board.
        double swX = bb.getHoleX(0) - 60;
        ToggleSwitch swA = new ToggleSwitch();
        swA.setPosition(swX, bb.getBoardY() + 340);
        ToggleSwitch swB = new ToggleSwitch();
        swB.setPosition(swX, bb.getBoardY() + 380);

        // Shared A/B bus columns on the board, before each chip.
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

        // Rails
        c.addWire(new Wire(vcc.getPin("VCC"),
                bb.getTopPositive().getPoint(0).getOrCreateHolePin()));
        c.addWire(new Wire(gnd.getPin("GND"),
                bb.getBottomNegative().getPoint(29).getOrCreateHolePin()));

        // IC power
        wireIcPower(c, bb, xorCol);
        wireIcPower(c, bb, andCol);

        // Switches → bus columns A, B on row j
        c.addWire(route(new Wire(swA.getPin("OUT"),
                bb.getHole(colA, 9).getOrCreateHolePin()),
                belowBoard(bb, 1)));
        c.addWire(route(new Wire(swB.getPin("OUT"),
                bb.getHole(colB, 9).getOrCreateHolePin()),
                belowBoard(bb, 3)));

        // Bus A → XOR pin 1 and AND pin 1 (chained hop on row g: colA → xor → and)
        c.addWire(hop(bb, colA, xorCol, 6));
        c.addWire(hop(bb, xorCol, andCol, 6));
        // Bus B → XOR pin 2 and AND pin 2 (chained hop on row h: colB → xor+1 → and+1)
        c.addWire(hop(bb, colB, xorCol + 1, 7));
        c.addWire(hop(bb, xorCol + 1, andCol + 1, 7));

        // SUM: XOR pin 3 → r1 → sumLed → GND rail
        c.addWire(new Wire(
                bb.getHole(xorCol + 2, 9).getOrCreateHolePin(), r1.getPin("1")));
        c.addWire(new Wire(r1.getPin("2"), sumLed.getPin("anode")));
        c.addWire(route(new Wire(sumLed.getPin("cathode"),
                bb.getBottomNegative().getPoint(16).getOrCreateHolePin()),
                belowBoard(bb, 0)));

        // CARRY: AND pin 3 → r2 → carryLed → GND rail
        c.addWire(new Wire(
                bb.getHole(andCol + 2, 9).getOrCreateHolePin(), r2.getPin("1")));
        c.addWire(new Wire(r2.getPin("2"), carryLed.getPin("anode")));
        c.addWire(route(new Wire(carryLed.getPin("cathode"),
                bb.getBottomNegative().getPoint(29).getOrCreateHolePin()),
                belowBoard(bb, 2)));

        // Labels
        swA.setDisplayLabel("A");
        swB.setDisplayLabel("B");
        sumLed.setDisplayLabel("SUM");
        carryLed.setDisplayLabel("CARRY");

        // Test cases — 2 inputs, all 4 combinations
        setTests(c,
                new String[]{"00", "01", "10", "11"},
                new String[]{"00", "10", "10", "01"});  // SUM, CARRY

        c.setModified(false);
        return c;
    }

    // ----- Demo: Binary → Gray Code (4-bit) -----

    /**
     * G3 = B3
     * G2 = B3 ^ B2
     * G1 = B2 ^ B1
     * G0 = B1 ^ B0
     *
     * Uses one 7486:
     *  Gate A (cols, f-net): B3, B2 → G2
     *  Gate B (cols, f-net): B2, B1 → G1
     *  Gate C (cols, e-net): B1, B0 → G0
     *
     * Bit convention: DIPSwitch OUT1..OUT4 maps left → right = MSB → LSB,
     * so OUT1=B3, OUT2=B2, OUT3=B1, OUT4=B0. Light-bar IN0..IN3 is read
     * left → right = G3..G0 the same way.
     */
    private static Circuit buildBinaryToGrayDemo() {
        Circuit c = new Circuit();
        c.setName("Demo: Binary \u2192 Gray Code");
        Breadboard bb = c.getBreadboard();
        ensurePositions(bb);

        int xorCol = 10;
        XORGate7486 xorIc = new XORGate7486();
        xorIc.setPosition(bb.getHoleX(xorCol) - 10, bb.getHoleY(4));
        attachIcSlots(c, bb, xorIc, xorCol);

        // Input bus columns (B3..B0) — left of the chip
        int colB3 = 2, colB2 = 4, colB1 = 6, colB0 = 8;
        // Output bus columns (G3..G0) — right of the chip
        int colG3 = 19, colG2 = 21, colG1 = 23, colG0 = 25;

        DIPSwitch dip = new DIPSwitch(4);
        // Switch positions: OUT1..OUT4 pins at x+16, x+31, x+46, x+61 relative
        // to the DIP body. Place so the four pins line up below colB3..colB0.
        // Target pin X's: holeX(colB3), holeX(colB2), holeX(colB1), holeX(colB0)
        // The DIP pitch is 15px; board pitch is 20px — the runs will have to
        // spread out horizontally. Put the DIP slightly left of colB3 so the
        // leftmost switch is below colB3.
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

        // Rails
        c.addWire(new Wire(vcc.getPin("VCC"),
                bb.getTopPositive().getPoint(0).getOrCreateHolePin()));
        c.addWire(new Wire(gnd.getPin("GND"),
                bb.getBottomNegative().getPoint(29).getOrCreateHolePin()));

        // IC power
        wireIcPower(c, bb, xorCol);

        // --- DIP → input bus columns (row j) ---
        // OUT1=B3, OUT2=B2, OUT3=B1, OUT4=B0 — leftmost switch is MSB.
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

        // --- Gate A: B3, B2 → G2 (pins 1, 2 → 3 on f-net) — on-board hops ---
        c.addWire(hop(bb, colB3, xorCol, 6));         // B3 → pin 1
        c.addWire(hop(bb, colB2, xorCol + 1, 7));     // B2 → pin 2
        c.addWire(hop(bb, xorCol + 2, colG2, 8));     // pin 3 → G2

        // --- Gate B: B2, B1 → G1 (pins 4, 5 → 6 on f-net) — on-board hops ---
        c.addWire(hop(bb, colB2, xorCol + 3, 6));     // B2 → pin 4 (chains with B2 above via col net)
        c.addWire(hop(bb, colB1, xorCol + 4, 7));     // B1 → pin 5
        c.addWire(hop(bb, xorCol + 5, colG1, 8));     // pin 6 → G1

        // --- Gate C: B1, B0 → G0 (pins 9, 10 → 8 on e-net). The sources are
        // on the f-net bus columns below the chip, so these wires must still
        // cross the middle gap — keep them as above-board routes. ---
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

        // --- G3 = B3 passes straight through — one long on-board hop ---
        c.addWire(hop(bb, colB3, colG3, 6));

        // --- LightBar IN0..IN3 ← G3..G0 (leftmost = MSB) ---
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

        // Labels
        dip.setTag(0, "B3"); dip.setTag(1, "B2"); dip.setTag(2, "B1"); dip.setTag(3, "B0");
        bar.setTag(0, "G3"); bar.setTag(1, "G2"); bar.setTag(2, "G1"); bar.setTag(3, "G0");

        // Test cases — 4 inputs (16 combos ≤ 32, write all)
        // Binary B3B2B1B0 → Gray G3G2G1G0: G3=B3, Gi=Bi^B(i+1)
        // Output columns: G3,G2,G1,G0,IN4..IN7 — last 4 are unused (D=don't care)
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

    // ----- Demo: Gray Code → Binary (4-bit) -----

    /**
     * B3 = G3
     * B2 = G2 ^ B3
     * B1 = G1 ^ B2
     * B0 = G0 ^ B1
     *
     * One 7486 — the gates chain each output into the next stage.
     */
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
        // BCD pin pitch is 15; left pin is B3 (MSB). Centre under colB3.
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

        // Rails
        c.addWire(new Wire(vcc.getPin("VCC"),
                bb.getTopPositive().getPoint(0).getOrCreateHolePin()));
        c.addWire(new Wire(gnd.getPin("GND"),
                bb.getBottomNegative().getPoint(29).getOrCreateHolePin()));

        // IC power
        wireIcPower(c, bb, xorCol);

        // DIP → G3..G0 (leftmost switch OUT1 = MSB)
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

        // B3 = G3 pass-through — long on-board hop
        c.addWire(hop(bb, colG3, colB3, 6));

        // Gate A: G2 ^ G3 → B2 — on-board hops
        c.addWire(hop(bb, colG2, xorCol, 7));         // G2 → pin 1
        c.addWire(hop(bb, colG3, xorCol + 1, 8));     // G3 → pin 2
        c.addWire(hop(bb, xorCol + 2, colB2, 6));     // pin 3 → B2

        // Gate B: G1 ^ B2 → B1 — on-board hops
        c.addWire(hop(bb, colG1, xorCol + 3, 7));     // G1 → pin 4
        c.addWire(hop(bb, colB2, xorCol + 4, 8));     // B2 → pin 5
        c.addWire(hop(bb, xorCol + 5, colB1, 6));     // pin 6 → B1

        // Gate C: G0 ^ B1 → B0 (e-net, routed above the board)
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

        // LightBar IN0..IN3 ← B3..B0 (leftmost = MSB)
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

        // BCD display B3..B0 (left pin = B3 = MSB)
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

        // Labels
        dip.setTag(0, "G3"); dip.setTag(1, "G2"); dip.setTag(2, "G1"); dip.setTag(3, "G0");
        bar.setTag(0, "B3"); bar.setTag(1, "B2"); bar.setTag(2, "B1"); bar.setTag(3, "B0");

        // Test cases — 4 inputs (16 combos ≤ 32, write all)
        // Gray G3G2G1G0 → Binary B3B2B1B0: B3=G3, Bi=G(i)^B(i+1)
        // Output columns: B3,B2,B1,B0,IN4..IN7 — last 4 unused (D)
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

    // ----- Demo: 4-bit Ripple Adder across two breadboards -----

    /**
     * Per-bit full adder:
     *   P_i = A_i ^ B_i         (XOR)
     *   S_i = P_i ^ C_i         (XOR)
     *   G_i = A_i & B_i         (AND)
     *   T_i = P_i & C_i         (AND)
     *   C_{i+1} = G_i | T_i     (OR)
     *
     * Chips (all DIP-14, 7 columns wide):
     *   bb1: XOR1 (col 4), XOR2 (col 13), AND1 (col 22)
     *   bb2: AND2 (col 4), OR (col 13)
     *
     * Bit convention: DIPSwitch OUT1..OUT4 = MSB..LSB, LightBar IN0..IN3 =
     * Cout..S0 shown left (MSB/Cout) to right (LSB) — matches the project
     * convention that the leftmost pin/cell of an array is the most
     * significant.
     */
    private static Circuit buildRippleAdderDemo() {
        Circuit c = new Circuit();
        c.setName("Demo: 4-bit Ripple Adder");

        Breadboard bb1 = c.getBreadboard();
        ensurePositions(bb1);

        Breadboard bb2 = new Breadboard(30);
        bb2.setPosition(bb1.getBoardX() + bb1.getBoardWidth() + 40, bb1.getBoardY());
        c.addBreadboard(bb2);
        ensurePositions(bb2);

        // --- Chips ---
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

        // --- Power and rails ---
        PowerSupply5V vcc = new PowerSupply5V();
        vcc.setPosition(bb1.getHoleX(0) - 17, bb1.getBoardY() - 40);
        Ground gnd = new Ground();
        gnd.setPosition(bb2.getHoleX(29) - 17, bb2.getBoardY() + 395);
        c.addComponent(vcc);
        c.addComponent(gnd);

        // 5V → bb1 top+; GND → bb2 bot-
        c.addWire(new Wire(vcc.getPin("VCC"),
                bb1.getTopPositive().getPoint(0).getOrCreateHolePin()));
        c.addWire(new Wire(gnd.getPin("GND"),
                bb2.getBottomNegative().getPoint(29).getOrCreateHolePin()));

        // Tie the two boards' rails together so both sides share 5V / GND.
        // These run above / below the boards in their own lanes so they don't
        // cross any signals.
        c.addWire(route(new Wire(
                bb1.getTopPositive().getPoint(29).getOrCreateHolePin(),
                bb2.getTopPositive().getPoint(0).getOrCreateHolePin()),
                aboveBoard(bb1, 0)));
        c.addWire(route(new Wire(
                bb1.getBottomNegative().getPoint(29).getOrCreateHolePin(),
                bb2.getBottomNegative().getPoint(0).getOrCreateHolePin()),
                belowBoard(bb1, 0)));

        // Power each IC
        wireIcPower(c, bb1, xor1Col);
        wireIcPower(c, bb1, xor2Col);
        wireIcPower(c, bb1, and1Col);
        wireIcPower(c, bb2, and2Col);
        wireIcPower(c, bb2, orCol);

        // --- Input bus columns on bb1 ---
        int cA3 = 0, cA2 = 1, cA1 = 2, cA0 = 3;        // A bus (left of XOR1)
        int cB3 = 12, cB2 = 11, cB1 = 20, cB0 = 21;    // B bus spread across bb1 free cols
        int cCin = 29;                                  // Carry-in bus on bb1 right

        // --- Input devices ---
        DIPSwitch dipA = new DIPSwitch(4);
        dipA.setPosition(bb1.getHoleX(0) - 5, bb1.getBoardY() + 340);
        c.addComponent(dipA);

        DIPSwitch dipB = new DIPSwitch(4);
        dipB.setPosition(bb1.getHoleX(10) - 5, bb1.getBoardY() + 340);
        c.addComponent(dipB);

        ToggleSwitch swCin = new ToggleSwitch();
        swCin.setPosition(bb1.getHoleX(29) - 50, bb1.getBoardY() + 340);
        c.addComponent(swCin);

        // DIP A: OUT1..OUT4 = A3..A0 (left pin is MSB)
        c.addWire(route(new Wire(dipA.getPin("OUT1"), bb1.getHole(cA3, 9).getOrCreateHolePin()),
                belowBoard(bb1, 2)));
        c.addWire(route(new Wire(dipA.getPin("OUT2"), bb1.getHole(cA2, 9).getOrCreateHolePin()),
                belowBoard(bb1, 4)));
        c.addWire(route(new Wire(dipA.getPin("OUT3"), bb1.getHole(cA1, 9).getOrCreateHolePin()),
                belowBoard(bb1, 6)));
        c.addWire(route(new Wire(dipA.getPin("OUT4"), bb1.getHole(cA0, 9).getOrCreateHolePin()),
                belowBoard(bb1, 8)));

        // DIP B: OUT1..OUT4 = B3..B0
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

        // --- Output LightBar below bb2 ---
        LightBar bar = new LightBar();
        bar.setPosition(bb2.getHoleX(6) - 15, bb2.getBoardY() + 340);
        c.addComponent(bar);

        // ===========================================================
        // bb1 XOR1: FA0 and FA1 XORs
        //   Gate A (f-net, xor1Col,+1 → +2)   : A0, B0 → P0
        //   Gate B (f-net, xor1Col+3,+4 → +5) : P0, Cin → S0
        //   Gate C (e-net, xor1Col+5,+4 → +6) : A1, B1 → P1
        //   Gate D (e-net, xor1Col+2,+1 → +3) : P1, C1  → S1
        // ===========================================================

        // Gate A inputs — on-board f-net hops
        c.addWire(hop(bb1, cA0, xor1Col, 6));
        c.addWire(hop(bb1, cB0, xor1Col + 1, 6));

        // Gate B: P0, Cin — on-board f-net hops
        c.addWire(hop(bb1, xor1Col + 2, xor1Col + 3, 6));
        c.addWire(hop(bb1, cCin, xor1Col + 4, 6));

        // Gate C (e-net) inputs — sources are f-net bus columns, so must
        // still cross the middle gap via above-board routes.
        c.addWire(route(new Wire(
                bb1.getHole(cA1, 9).getOrCreateHolePin(),
                bb1.getHole(xor1Col + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 2)));
        c.addWire(route(new Wire(
                bb1.getHole(cB1, 9).getOrCreateHolePin(),
                bb1.getHole(xor1Col + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 4)));

        // Gate D (e-net): P1 → xor1Col+2 e — both on e-net, on-board hop
        c.addWire(hop(bb1, xor1Col + 6, xor1Col + 2, 3));

        // ===========================================================
        // bb1 XOR2: FA2 and FA3 XORs
        // ===========================================================

        // Gate A inputs — on-board f-net hops
        c.addWire(hop(bb1, cA2, xor2Col, 6));
        c.addWire(hop(bb1, cB2, xor2Col + 1, 6));

        // Gate B: P2, C2 — P2 → xor2Col+3, on-board f-net hop
        c.addWire(hop(bb1, xor2Col + 2, xor2Col + 3, 6));
        // C2 feeds xor2Col+4 f — added with the OR gate fanout below.

        // Gate C (e-net) inputs — sources f-net, still need above-board routes
        c.addWire(route(new Wire(
                bb1.getHole(cA3, 9).getOrCreateHolePin(),
                bb1.getHole(xor2Col + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 8)));
        c.addWire(route(new Wire(
                bb1.getHole(cB3, 9).getOrCreateHolePin(),
                bb1.getHole(xor2Col + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 10)));

        // Gate D (e-net): P3 → xor2Col+2 e — both on e-net, on-board hop
        c.addWire(hop(bb1, xor2Col + 6, xor2Col + 2, 3));
        // C3 lands on xor2Col+1 e — added with OR gate fanout below.

        // ===========================================================
        // bb1 AND1: FA0/FA1 ANDs
        // ===========================================================

        // Gate A inputs (FA0) — on-board f-net hops
        c.addWire(hop(bb1, cA0, and1Col, 7));
        c.addWire(hop(bb1, cB0, and1Col + 1, 7));

        // Gate B inputs (FA0 P0 & Cin → T0) — on-board f-net hops
        c.addWire(hop(bb1, xor1Col + 2, and1Col + 3, 7));
        c.addWire(hop(bb1, cCin, and1Col + 4, 7));

        // Gate C (e-net) inputs (FA1 A1 & B1) — sources f-net, above-board routes
        c.addWire(route(new Wire(
                bb1.getHole(cA1, 9).getOrCreateHolePin(),
                bb1.getHole(and1Col + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 14)));
        c.addWire(route(new Wire(
                bb1.getHole(cB1, 9).getOrCreateHolePin(),
                bb1.getHole(and1Col + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 16)));

        // Gate D (e-net) input (FA1 P1) — both on e-net, on-board hop
        c.addWire(hop(bb1, xor1Col + 6, and1Col + 2, 3));
        // C1 lands on and1Col+1 e — added with OR gate fanout below.

        // ===========================================================
        // bb2 AND2: FA2/FA3 ANDs
        // ===========================================================

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

        // ===========================================================
        // bb2 OR: carry ORs
        //   Gate A (f): G0, T0 → C1
        //   Gate B (f): G1, T1 → C2
        //   Gate C (e): G2, T2 → C3
        //   Gate D (e): G3, T3 → Cout
        // ===========================================================

        // Gate A: G0, T0
        c.addWire(route(new Wire(
                bb1.getHole(and1Col + 2, 9).getOrCreateHolePin(),
                bb2.getHole(orCol, 9).getOrCreateHolePin()),
                belowBoard(bb1, 48)));
        c.addWire(route(new Wire(
                bb1.getHole(and1Col + 5, 9).getOrCreateHolePin(),
                bb2.getHole(orCol + 1, 9).getOrCreateHolePin()),
                belowBoard(bb1, 50)));
        // C1 fan-out: bb2 orCol+2 f → bb1 xor1Col+1 e (Gate D) and bb1 and1Col+1 e (Gate D)
        c.addWire(route(new Wire(
                bb2.getHole(orCol + 2, 9).getOrCreateHolePin(),
                bb1.getHole(xor1Col + 1, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 26)));
        c.addWire(route(new Wire(
                bb2.getHole(orCol + 2, 9).getOrCreateHolePin(),
                bb1.getHole(and1Col + 1, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 28)));

        // Gate B: G1, T1
        c.addWire(route(new Wire(
                bb1.getHole(and1Col + 6, 0).getOrCreateHolePin(),
                bb2.getHole(orCol + 3, 9).getOrCreateHolePin()),
                aboveBoard(bb1, 30)));
        c.addWire(route(new Wire(
                bb1.getHole(and1Col + 3, 0).getOrCreateHolePin(),
                bb2.getHole(orCol + 4, 9).getOrCreateHolePin()),
                aboveBoard(bb1, 32)));
        // C2 fan-out: bb2 orCol+5 f → bb1 xor2Col+4 f (Gate B) and bb2 and2Col+4 f (Gate B)
        c.addWire(route(new Wire(
                bb2.getHole(orCol + 5, 9).getOrCreateHolePin(),
                bb1.getHole(xor2Col + 4, 9).getOrCreateHolePin()),
                belowBoard(bb1, 52)));
        c.addWire(hop(bb2, orCol + 5, and2Col + 4, 6));

        // Gate C (e): G2, T2
        c.addWire(route(new Wire(
                bb2.getHole(and2Col + 2, 9).getOrCreateHolePin(),
                bb2.getHole(orCol + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb2, 0)));
        c.addWire(route(new Wire(
                bb2.getHole(and2Col + 5, 9).getOrCreateHolePin(),
                bb2.getHole(orCol + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb2, 2)));
        // C3 fan-out: bb2 orCol+6 e → bb1 xor2Col+1 e and bb2 and2Col+1 e
        c.addWire(route(new Wire(
                bb2.getHole(orCol + 6, 0).getOrCreateHolePin(),
                bb1.getHole(xor2Col + 1, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 34)));
        c.addWire(hop(bb2, orCol + 6, and2Col + 1, 3));

        // Gate D (e): G3, T3 → Cout — both legs are same-board e-net hops
        c.addWire(hop(bb2, and2Col + 6, orCol + 2, 3));
        c.addWire(hop(bb2, and2Col + 3, orCol + 1, 2));

        // ===========================================================
        // Outputs → LightBar
        // LightBar pins read left → right as MSB → LSB:
        //   IN0 = Cout, IN1 = S3, IN2 = S2, IN3 = S1, IN4 = S0
        // ===========================================================
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

        // Labels
        dipA.setTag(0, "A3"); dipA.setTag(1, "A2"); dipA.setTag(2, "A1"); dipA.setTag(3, "A0");
        dipB.setTag(0, "B3"); dipB.setTag(1, "B2"); dipB.setTag(2, "B1"); dipB.setTag(3, "B0");
        swCin.setDisplayLabel("Cin");
        bar.setTag(0, "Cout"); bar.setTag(1, "S3"); bar.setTag(2, "S2"); bar.setTag(3, "S1"); bar.setTag(4, "S0");

        // Test cases — 9 inputs (512 combos > 32), write 16 representative rows
        // Inputs: A3,A2,A1,A0, B3,B2,B1,B0, Cin  (MSB first in each group)
        // Outputs: Cout,S3,S2,S1,S0, IN5..IN7 (last 3 D=don't care)
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

    // ----- Demo: NAND Logic (4-bit) -----

    /**
     * Four boolean functions implemented purely with NAND gates:
     *   Z = C'                          — 1 gate  (NAND as inverter)
     *   X = A + CD                      — 3 gates (DeMorgan)
     *   W = AB + ACD                    — 3 gates (2-input + 3-input NAND)
     *   Y = A'C + BD + B'C'D'           — 7 gates (most complex output)
     *
     * Chips:
     *   bb1: NAND1 (7400, col 7)  — four inverters (C', A', B', D')
     *        NAND2 (7400, col 17) — X logic, (AB)', and W final
     *   bb2: NAND3 (7400, col 4)  — Y two-input intermediates (A'C)', (BD)'
     *        NAND3IN (7410, col 15) — three-input: (ACD)', (B'C'D')', Y final
     */
    private static Circuit buildNandLogicDemo() {
        Circuit c = new Circuit();
        c.setName("Demo: NAND Logic");

        Breadboard bb1 = c.getBreadboard();
        ensurePositions(bb1);

        Breadboard bb2 = new Breadboard(30);
        bb2.setPosition(bb1.getBoardX() + bb1.getBoardWidth() + 40, bb1.getBoardY());
        c.addBreadboard(bb2);
        ensurePositions(bb2);

        // --- Chips ---
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

        // --- Power and rails ---
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

        // Tie rails across boards
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

        // --- Input bus columns on bb1 ---
        int cA = 0, cB = 1, cC = 2, cD = 3;

        DIPSwitch dip = new DIPSwitch(4);
        dip.setPosition(bb1.getHoleX(0) - 5, bb1.getBoardY() + 340);
        c.addComponent(dip);

        // DIP OUT1..OUT4 = A..D (leftmost = A)
        c.addWire(route(new Wire(dip.getPin("OUT1"), bb1.getHole(cA, 9).getOrCreateHolePin()),
                belowBoard(bb1, 2)));
        c.addWire(route(new Wire(dip.getPin("OUT2"), bb1.getHole(cB, 9).getOrCreateHolePin()),
                belowBoard(bb1, 4)));
        c.addWire(route(new Wire(dip.getPin("OUT3"), bb1.getHole(cC, 9).getOrCreateHolePin()),
                belowBoard(bb1, 6)));
        c.addWire(route(new Wire(dip.getPin("OUT4"), bb1.getHole(cD, 9).getOrCreateHolePin()),
                belowBoard(bb1, 8)));

        // --- Output LightBar below bb2 ---
        LightBar bar = new LightBar();
        bar.setPosition(bb2.getHoleX(22) - 15, bb2.getBoardY() + 340);
        c.addComponent(bar);

        // Output bus columns on bb2
        int colW = 22, colX = 24, colY = 26, colZ = 28;

        // ===========================================================
        // NAND1 (bb1 col 7): Inverters
        //   Gate A (f): pin1@7, pin2@8  → pin3@9   : C, C  → C' (=Z)
        //   Gate B (f): pin4@10, pin5@11 → pin6@12  : A, A  → A'
        //   Gate C (e): pin9@12, pin10@11 → pin8@13 : B, B  → B'
        //   Gate D (e): pin12@9, pin13@8  → pin11@10: D, D  → D'
        // ===========================================================

        // Gate A: C → pin1, C → pin2
        c.addWire(hop(bb1, cC, n1Col, 6));         // C → col 7 (row g)
        c.addWire(hop(bb1, n1Col, n1Col + 1, 7));  // col 7 → col 8 (row h, ties both inputs)

        // Gate B: A → pin4, A → pin5
        c.addWire(hop(bb1, cA, n1Col + 3, 8));     // A → col 10 (row i)
        c.addWire(hop(bb1, n1Col + 3, n1Col + 4, 7)); // col 10 → col 11 (row h)

        // Gate C: B → pin9(col12,e) and pin10(col11,e)
        // B is on f-net at col1, must cross to e-net via above-board
        c.addWire(route(new Wire(
                bb1.getHole(cB, 9).getOrCreateHolePin(),
                bb1.getHole(n1Col + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 2)));
        c.addWire(hop(bb1, n1Col + 5, n1Col + 4, 3));  // col 12 → col 11 (row d)

        // Gate D: D → pin12(col9,e) and pin13(col8,e)
        // D is on f-net at col3, must cross to e-net
        c.addWire(route(new Wire(
                bb1.getHole(cD, 9).getOrCreateHolePin(),
                bb1.getHole(n1Col + 2, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 4)));
        c.addWire(hop(bb1, n1Col + 2, n1Col + 1, 2));  // col 9 → col 8 (row c)

        // ===========================================================
        // NAND2 (bb1 col 17): X and W computation
        //   Gate A (f): pin1@17, pin2@18  → pin3@19 : C, D  → (CD)'
        //   Gate B (f): pin4@20, pin5@21  → pin6@22 : A', (CD)' → X
        //   Gate C (e): pin9@22, pin10@21 → pin8@23 : A, B  → (AB)'
        //   Gate D (e): pin12@19, pin13@18 → pin11@20: (AB)', (ACD)' → W
        // ===========================================================

        // Gate A: C → pin1, D → pin2
        c.addWire(hop(bb1, cC, n2Col, 6));          // C → col 17 (row g)
        c.addWire(hop(bb1, cD, n2Col + 1, 8));      // D → col 18 (row i)

        // Gate B: A' → pin4, (CD)' → pin5
        // A' is at NAND1 col 12 (f-net, pin 6 output)
        c.addWire(hop(bb1, n1Col + 5, n2Col + 3, 6)); // A'(col 12) → col 20 (row g)
        // (CD)' is at col 19 (pin 3 output), same chip → short hop
        c.addWire(hop(bb1, n2Col + 2, n2Col + 4, 7)); // (CD)'(col 19) → col 21 (row h)

        // Gate C: A → pin9(col22,e), B → pin10(col21,e)
        // A and B are on f-net, must cross to e-net
        c.addWire(route(new Wire(
                bb1.getHole(cA, 9).getOrCreateHolePin(),
                bb1.getHole(n2Col + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 6)));
        c.addWire(route(new Wire(
                bb1.getHole(cB, 9).getOrCreateHolePin(),
                bb1.getHole(n2Col + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 8)));

        // Gate D: (AB)' → pin12(col19,e), (ACD)' → pin13(col18,e)
        // (AB)' at pin8 col23(e) — hop on e-net to col19
        c.addWire(hop(bb1, n2Col + 6, n2Col + 2, 3)); // (AB)' col 23 → col 19 (row d)
        // (ACD)' comes from bb2 NAND3IN Gate 1 → cross-board to col18(e)
        // (wired after NAND3IN section below)

        // ===========================================================
        // NAND3 (bb2 col 4): Y two-input intermediates
        //   Gate A (f): pin1@4, pin2@5   → pin3@6  : A', C  → (A'C)'
        //   Gate B (f): pin4@7, pin5@8   → pin6@9  : B, D   → (BD)'
        // ===========================================================

        // Gate A: A' → pin1(col4,f), C → pin2(col5,f)
        // A' from bb1 NAND1 col 12 (f-net)
        c.addWire(route(new Wire(
                bb1.getHole(n1Col + 5, 9).getOrCreateHolePin(),
                bb2.getHole(n3Col, 9).getOrCreateHolePin()),
                belowBoard(bb1, 10)));
        // C from bb1 col 2 (f-net)
        c.addWire(route(new Wire(
                bb1.getHole(cC, 9).getOrCreateHolePin(),
                bb2.getHole(n3Col + 1, 9).getOrCreateHolePin()),
                belowBoard(bb1, 12)));

        // Gate B: B → pin4(col7,f), D → pin5(col8,f)
        c.addWire(route(new Wire(
                bb1.getHole(cB, 9).getOrCreateHolePin(),
                bb2.getHole(n3Col + 3, 9).getOrCreateHolePin()),
                belowBoard(bb1, 14)));
        c.addWire(route(new Wire(
                bb1.getHole(cD, 9).getOrCreateHolePin(),
                bb2.getHole(n3Col + 4, 9).getOrCreateHolePin()),
                belowBoard(bb1, 16)));

        // ===========================================================
        // NAND3IN (7410, bb2 col 15): Three-input NAND gates
        //   Gate 1: pin1@15(f), pin2@16(f), pin13@16(e) → pin12@17(e)
        //           A, C, D → (ACD)'
        //   Gate 2: pin3@17(f), pin4@18(f), pin5@19(f) → pin6@20(f)
        //           B', C', D' → (B'C'D')'
        //   Gate 3: pin9@20(e), pin10@19(e), pin11@18(e) → pin8@21(e)
        //           (A'C)', (BD)', (B'C'D')' → Y
        // ===========================================================

        // Gate 1: A → pin1(col15,f)
        c.addWire(route(new Wire(
                bb1.getHole(cA, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol, 9).getOrCreateHolePin()),
                belowBoard(bb1, 18)));
        // C → pin2(col16,f)
        c.addWire(route(new Wire(
                bb1.getHole(cC, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 1, 9).getOrCreateHolePin()),
                belowBoard(bb1, 20)));
        // D → pin13(col16,e) — must land on e-net (row a)
        c.addWire(route(new Wire(
                bb1.getHole(cD, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 1, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 10)));

        // Gate 2: B' → pin3(col17,f)
        // B' from NAND1 pin8 at bb1 col13(e-net) → need to reach bb2 col17(f)
        c.addWire(route(new Wire(
                bb1.getHole(n1Col + 6, 0).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 2, 9).getOrCreateHolePin()),
                aboveBoard(bb1, 12)));
        // C' → pin4(col18,f)  — C' from NAND1 pin3 at bb1 col9(f)
        c.addWire(route(new Wire(
                bb1.getHole(n1Col + 2, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 3, 9).getOrCreateHolePin()),
                belowBoard(bb1, 22)));
        // D' → pin5(col19,f) — D' from NAND1 pin11 at bb1 col10(e)
        c.addWire(route(new Wire(
                bb1.getHole(n1Col + 3, 0).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 4, 9).getOrCreateHolePin()),
                aboveBoard(bb1, 14)));

        // Gate 3 inputs:
        // (A'C)' from NAND3 pin3 at bb2 col6(f) → pin11(col18,e)
        c.addWire(route(new Wire(
                bb2.getHole(n3Col + 2, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 3, 0).getOrCreateHolePin()),
                aboveBoard(bb2, 0)));
        // (BD)' from NAND3 pin6 at bb2 col9(f) → pin10(col19,e)
        c.addWire(route(new Wire(
                bb2.getHole(n3Col + 5, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 4, 0).getOrCreateHolePin()),
                aboveBoard(bb2, 2)));
        // (B'C'D')' from Gate 2 pin6 at bb2 col20(f) → pin9(col20,e)
        // Same column, different net — route above board
        c.addWire(route(new Wire(
                bb2.getHole(n3iCol + 5, 9).getOrCreateHolePin(),
                bb2.getHole(n3iCol + 5, 0).getOrCreateHolePin()),
                aboveBoard(bb2, 4)));

        // ===========================================================
        // (ACD)' fanout: bb2 NAND3IN pin12 at col17(e) → bb1 NAND2 pin13 at col18(e)
        // ===========================================================
        c.addWire(route(new Wire(
                bb2.getHole(n3iCol + 2, 0).getOrCreateHolePin(),
                bb1.getHole(n2Col + 1, 0).getOrCreateHolePin()),
                aboveBoard(bb1, 16)));

        // ===========================================================
        // Outputs → LightBar
        //   IN0 = W, IN1 = X, IN2 = Y, IN3 = Z (leftmost = MSB)
        // ===========================================================

        // W: NAND2 pin11 at bb1 col20(e) → bb2 colW(f) bus → LightBar
        c.addWire(route(new Wire(
                bb1.getHole(n2Col + 3, 0).getOrCreateHolePin(),
                bb2.getHole(colW, 9).getOrCreateHolePin()),
                aboveBoard(bb1, 18)));

        // X: NAND2 pin6 at bb1 col22(f) → bb2 colX bus → LightBar
        c.addWire(route(new Wire(
                bb1.getHole(n2Col + 5, 9).getOrCreateHolePin(),
                bb2.getHole(colX, 9).getOrCreateHolePin()),
                belowBoard(bb1, 24)));

        // Y: NAND3IN pin8 at bb2 col21(e) → bb2 colY(f) bus
        // Must cross from e-net to f-net, so route above board to a free f-net col
        c.addWire(route(new Wire(
                bb2.getHole(n3iCol + 6, 0).getOrCreateHolePin(),
                bb2.getHole(colY, 9).getOrCreateHolePin()),
                aboveBoard(bb2, 6)));

        // Z: C' at NAND1 pin3, bb1 col9(f) → bb2 colZ bus
        c.addWire(route(new Wire(
                bb1.getHole(n1Col + 2, 9).getOrCreateHolePin(),
                bb2.getHole(colZ, 9).getOrCreateHolePin()),
                belowBoard(bb1, 26)));

        // LightBar IN0..IN3 ← W,X,Y,Z
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

        // Labels
        dip.setTag(0, "A"); dip.setTag(1, "B"); dip.setTag(2, "C"); dip.setTag(3, "D");
        bar.setTag(0, "W"); bar.setTag(1, "X"); bar.setTag(2, "Y"); bar.setTag(3, "Z");

        // Test cases — 4 inputs (16 combos ≤ 32, write all)
        // Z=C', X=A+CD, W=AB+ACD, Y=A'C+BD+B'C'D'
        // Output columns: W,X,Y,Z, IN4..IN7 (last 4 D=don't care)
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

    // ====================================================================
    // Helpers
    // ====================================================================

    /** IC-to-breadboard "slot wires": each IC pin to the HolePin beneath it. */
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

    /**
     * Wire a DIP-14 chip's VCC (pin 14) and GND (pin 7) to the board's
     * rails. Pin 14 sits in the 'e' row of column {@code icCol} — we hop
     * onto that column's a-e net via row 'a' and connect to the top + rail
     * directly above, so the wire is a short vertical stalk. Pin 7 is in
     * the 'f' row of column {@code icCol+6} — hop onto that column's f-j
     * net via row 'j' and connect to the bottom − rail directly below.
     */
    private static void wireIcPower(Circuit c, Breadboard bb, int icCol) {
        c.addWire(new Wire(
                bb.getTopPositive().getPoint(icCol).getOrCreateHolePin(),
                bb.getHole(icCol, 0).getOrCreateHolePin()));
        c.addWire(new Wire(
                bb.getBottomNegative().getPoint(icCol + 6).getOrCreateHolePin(),
                bb.getHole(icCol + 6, 9).getOrCreateHolePin()));
    }

    /** Paint once to a throwaway canvas so rail/hole ContactPoints get their canvas positions. */
    private static void ensurePositions(Breadboard bb) {
        bb.render(new Canvas(1, 1).getGraphicsContext2D());
    }

    // --- Routing lane helpers ------------------------------------------------

    /**
     * Add two orthogonal waypoints so that the wire travels as
     *   start → (startX, laneY) → (endX, laneY) → end.
     * When several wires share a region, give each a unique laneY so their
     * horizontal segments don't overlap.
     */
    private static Wire route(Wire w, double laneY) {
        double sx = w.getStartPin().getX();
        double ex = w.getEndPin().getX();
        w.getWaypoints().add(new double[]{sx, laneY});
        w.getWaypoints().add(new double[]{ex, laneY});
        return w;
    }

    /**
     * Lane Y below the bottom − rail, indexed from 0 upward. Lane 0 sits
     * just below the rail; higher indices move further down toward the
     * input device area. Step is tight (5 px) to fit plenty of lanes.
     */
    private static double belowBoard(Breadboard bb, int idx) {
        return bb.getBoardY() + 308 + idx * 5;
    }

    /**
     * Lane Y above the top + rail, indexed from 0 upward. Lane 0 sits just
     * above the rail; higher indices move further up. Used for signals
     * that need to travel along the top ("e-net") side of the chips.
     */
    private static double aboveBoard(Breadboard bb, int idx) {
        return bb.getBoardY() - 8 - idx * 5;
    }

    /**
     * Short on-board cross-column wire. Connects two holes on the same
     * column-net half via a direct horizontal run — no waypoints, no
     * routing above or below the board. This is the breadboard equivalent
     * of dropping a single jumper wire between two columns.
     *
     * <p>Pass {@code letter=6} (row 'g') for f-j net connections (where
     * pin 1-7 of a chip sits) — the wire runs just above the chip body.
     * Pass {@code letter=3} (row 'd') for a-e net connections (where
     * pin 14-8 of a chip sits) — the wire runs just below the chip body.
     * Use {@code letter=7}/{@code 8} or {@code 1}/{@code 2} as alternate
     * lanes when several hops in the same half would otherwise stack on
     * the same row.
     *
     * <p>Both endpoint holes must share a net half, otherwise the wire
     * will be forced to cut across the chip/middle gap and the visual
     * will break.
     */
    private static Wire hop(Breadboard bb, int colA, int colB, int letter) {
        return new Wire(
                bb.getHole(colA, letter).getOrCreateHolePin(),
                bb.getHole(colB, letter).getOrCreateHolePin());
    }

    // --- Test-case helpers ---------------------------------------------------

    /** Parse a binary string like "1010" into a LogicState array (HIGH/LOW). */
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

    /** Build saved test-case lists and attach them to the circuit. */
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
