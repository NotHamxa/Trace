package com.trace;

import com.trace.model.*;
import com.trace.model.chips.ANDGate7408;
import com.trace.model.input.ToggleSwitch;
import com.trace.model.output.LED;
import com.trace.model.passive.Resistor;
import com.trace.model.power.Ground;
import com.trace.model.power.PowerSupply5V;
import javafx.scene.paint.Color;

import java.io.File;

/**
 * 7408 AND demo, wired the way you'd build it on a real breadboard:
 * everything connects to the column nets the IC pins sit in (and the
 * bottom power rails), never directly to IC pins.
 */
public class DemoIcBuilder {
    public static void main(String[] args) throws Exception {
        Circuit circuit = new Circuit();
        circuit.setName("Demo: 7408 AND gate");
        Breadboard bb = circuit.getBreadboard();

        // --- Place the IC straddling rows e/f starting at column icCol ---
        ANDGate7408 ic = new ANDGate7408();
        int icCol = 9;
        ic.setPosition(bb.getHoleX(icCol) - 10, bb.getHoleY(4));

        // Force rail ContactPoints to receive their canvas positions (normally
        // done by Breadboard.render). Without this, getCanvasX/Y are 0.
        bb.render(new javafx.scene.canvas.Canvas(1, 1).getGraphicsContext2D());

        // Wire each IC pin to the HolePin of the hole beneath it. These "slot
        // wires" run through the IC's footprint and carry its state onto the
        // underlying column net — same architecture CanvasView uses for live
        // placement.
        // Pin 1..7  → bottom row (letter 5 = 'f'), columns icCol..icCol+6
        // Pin 14..8 → top    row (letter 4 = 'e'), columns icCol..icCol+6
        for (int i = 0; i < 7; i++) {
            circuit.addWire(new Wire(
                    ic.getPin(String.valueOf(i + 1)),
                    bb.getHole(icCol + i, 5).getOrCreateHolePin()));
            circuit.addWire(new Wire(
                    ic.getPin(String.valueOf(14 - i)),
                    bb.getHole(icCol + i, 4).getOrCreateHolePin()));
        }

        // --- Other components ---
        ToggleSwitch swA = new ToggleSwitch();
        swA.setPosition(40, 480);
        ToggleSwitch swB = new ToggleSwitch();
        swB.setPosition(40, 530);

        Resistor r = new Resistor(220);
        r.setPosition(bb.getHoleX(20) - 4, bb.getHoleY(5));
        LED led = new LED(Color.LIME);
        led.setPosition(bb.getHoleX(24) - 5, bb.getHoleY(5) + 20);

        PowerSupply5V vcc = new PowerSupply5V();
        vcc.setPosition(40, 580);
        Ground gnd = new Ground();
        gnd.setPosition(120, 580);

        circuit.addComponent(ic);
        circuit.addComponent(swA);
        circuit.addComponent(swB);
        circuit.addComponent(r);
        circuit.addComponent(led);
        circuit.addComponent(vcc);
        circuit.addComponent(gnd);

        // --- Power: 5V → bottom + rail, GND → bottom − rail ---
        circuit.addWire(new Wire(vcc.getPin("VCC"),
                bb.getBottomPositive().getPoint(2).getOrCreateHolePin()));
        circuit.addWire(new Wire(gnd.getPin("GND"),
                bb.getBottomNegative().getPoint(2).getOrCreateHolePin()));

        // --- Power the IC through column nets, NOT directly to its pins ---
        // VCC: bottom + rail → hole 'a' in pin14's column (same a-e net as pin 14)
        circuit.addWire(new Wire(
                bb.getBottomPositive().getPoint(icCol).getOrCreateHolePin(),
                bb.getHole(icCol, 0).getOrCreateHolePin()));
        // GND: bottom − rail → hole 'j' in pin7's column (same f-j net as pin 7)
        circuit.addWire(new Wire(
                bb.getBottomNegative().getPoint(icCol + 6).getOrCreateHolePin(),
                bb.getHole(icCol + 6, 9).getOrCreateHolePin()));

        // --- Inputs: switches → column holes for pins 1 and 2 (f-j net) ---
        circuit.addWire(new Wire(swA.getPin("OUT"),
                bb.getHole(icCol, 9).getOrCreateHolePin()));
        circuit.addWire(new Wire(swB.getPin("OUT"),
                bb.getHole(icCol + 1, 9).getOrCreateHolePin()));

        // --- Output: pin 3's column (icCol+2 f-j net) → resistor → LED → GND rail ---
        circuit.addWire(new Wire(
                bb.getHole(icCol + 2, 9).getOrCreateHolePin(),
                r.getPin("1")));
        circuit.addWire(new Wire(r.getPin("2"), led.getPin("anode")));
        circuit.addWire(new Wire(led.getPin("cathode"),
                bb.getBottomNegative().getPoint(25).getOrCreateHolePin()));

        File out = new File("demo_and_gate.trc");
        circuit.saveToFile(out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }
}
