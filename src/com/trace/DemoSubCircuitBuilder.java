package com.trace;

import com.trace.io.SubCircuitIO;
import com.trace.model.Breadboard;
import com.trace.model.Circuit;
import com.trace.model.Wire;
import com.trace.model.chips.XORGate7486;
import com.trace.model.ports.InputPort;
import com.trace.model.ports.OutputPort;
import com.trace.model.power.Ground;
import com.trace.model.power.PowerSupply5V;
import com.trace.model.subcircuit.SubCircuitDefinition;
import javafx.scene.canvas.Canvas;

import java.io.File;

/**
 * Generates demo_gray_to_binary.trs — a Gray Code → Binary 4-bit sub-circuit.
 *
 * Logic (one 7486 XOR chip):
 *   B3 = G3
 *   B2 = G2 ^ B3
 *   B1 = G1 ^ B2
 *   B0 = G0 ^ B1
 *
 * The inner circuit uses InputPort/OutputPort components so the saved .trs
 * can be dropped onto another canvas as a reusable 4-in / 4-out block.
 */
public class DemoSubCircuitBuilder {

    public static void main(String[] args) throws Exception {
        Circuit c = new Circuit();
        c.setName("Gray Code \u2192 Binary (4-bit)");
        Breadboard bb = c.getBreadboard();
        bb.render(new Canvas(1, 1).getGraphicsContext2D());

        int xorCol = 10;
        XORGate7486 xorIc = new XORGate7486();
        xorIc.setPosition(bb.getHoleX(xorCol) - 10, bb.getHoleY(4));
        attachIcSlots(c, bb, xorIc, xorCol);
        c.addComponent(xorIc);

        int colG3 = 2, colG2 = 4, colG1 = 6, colG0 = 8;
        int colB3 = 19, colB2 = 21, colB1 = 23, colB0 = 25;

        InputPort g3 = new InputPort("G3");
        InputPort g2 = new InputPort("G2");
        InputPort g1 = new InputPort("G1");
        InputPort g0 = new InputPort("G0");
        OutputPort b3 = new OutputPort("B3");
        OutputPort b2 = new OutputPort("B2");
        OutputPort b1 = new OutputPort("B1");
        OutputPort b0 = new OutputPort("B0");

        // Inputs down the left, outputs down the right. Y-order defines port order.
        double inX = bb.getHoleX(0) - 80;
        double outX = bb.getHoleX(29) + 30;
        double topY = bb.getBoardY() + 340;
        double pitch = 40;
        g3.setPosition(inX, topY);
        g2.setPosition(inX, topY + pitch);
        g1.setPosition(inX, topY + 2 * pitch);
        g0.setPosition(inX, topY + 3 * pitch);
        b3.setPosition(outX, topY);
        b2.setPosition(outX, topY + pitch);
        b1.setPosition(outX, topY + 2 * pitch);
        b0.setPosition(outX, topY + 3 * pitch);

        c.addComponent(g3); c.addComponent(g2); c.addComponent(g1); c.addComponent(g0);
        c.addComponent(b3); c.addComponent(b2); c.addComponent(b1); c.addComponent(b0);

        PowerSupply5V vcc = new PowerSupply5V();
        vcc.setPosition(bb.getHoleX(0) - 17, bb.getBoardY() - 40);
        Ground gnd = new Ground();
        gnd.setPosition(bb.getHoleX(29) - 17, bb.getBoardY() + 395);
        c.addComponent(vcc);
        c.addComponent(gnd);

        // Rails
        c.addWire(new Wire(vcc.getPin("VCC"),
                bb.getTopPositive().getPoint(0).getOrCreateHolePin()));
        c.addWire(new Wire(gnd.getPin("GND"),
                bb.getBottomNegative().getPoint(29).getOrCreateHolePin()));

        // IC power
        wireIcPower(c, bb, xorCol);

        // InputPorts → G3..G0 columns (row j)
        c.addWire(route(new Wire(g3.getPin("OUT"),
                bb.getHole(colG3, 9).getOrCreateHolePin()),
                belowBoard(bb, 1)));
        c.addWire(route(new Wire(g2.getPin("OUT"),
                bb.getHole(colG2, 9).getOrCreateHolePin()),
                belowBoard(bb, 3)));
        c.addWire(route(new Wire(g1.getPin("OUT"),
                bb.getHole(colG1, 9).getOrCreateHolePin()),
                belowBoard(bb, 5)));
        c.addWire(route(new Wire(g0.getPin("OUT"),
                bb.getHole(colG0, 9).getOrCreateHolePin()),
                belowBoard(bb, 7)));

        // B3 = G3 pass-through (long on-board hop on row g)
        c.addWire(hop(bb, colG3, colB3, 6));

        // Gate A: G2 ^ G3 → B2 — on-board hops
        c.addWire(hop(bb, colG2, xorCol, 7));         // G2 → pin 1
        c.addWire(hop(bb, colG3, xorCol + 1, 8));     // G3 → pin 2
        c.addWire(hop(bb, xorCol + 2, colB2, 6));     // pin 3 → B2

        // Gate B: G1 ^ B2 → B1 — on-board hops
        c.addWire(hop(bb, colG1, xorCol + 3, 7));     // G1 → pin 4
        c.addWire(hop(bb, colB2, xorCol + 4, 8));     // B2 → pin 5
        c.addWire(hop(bb, xorCol + 5, colB1, 6));     // pin 6 → B1

        // Gate C: G0 ^ B1 → B0 (route above the board across the chip middle)
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

        // Output ports ← B3..B0 columns (row j)
        c.addWire(route(new Wire(b3.getPin("IN"),
                bb.getHole(colB3, 9).getOrCreateHolePin()),
                belowBoard(bb, 9)));
        c.addWire(route(new Wire(b2.getPin("IN"),
                bb.getHole(colB2, 9).getOrCreateHolePin()),
                belowBoard(bb, 11)));
        c.addWire(route(new Wire(b1.getPin("IN"),
                bb.getHole(colB1, 9).getOrCreateHolePin()),
                belowBoard(bb, 13)));
        c.addWire(route(new Wire(b0.getPin("IN"),
                bb.getHole(colB0, 9).getOrCreateHolePin()),
                belowBoard(bb, 15)));

        c.setModified(false);

        SubCircuitDefinition def = new SubCircuitDefinition(
                "demo.gray2bin.v1",
                "Gray \u2192 Binary (4-bit)",
                c
        );

        File out = new File("demo_gray_to_binary.trs");
        SubCircuitIO.write(def, "Trace Demos", out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }

    private static void attachIcSlots(Circuit c, Breadboard bb, XORGate7486 ic, int icCol) {
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
}
