package com.trace;

import com.trace.model.*;
import com.trace.model.output.LED;
import com.trace.model.passive.Resistor;
import com.trace.model.power.Ground;
import com.trace.model.power.PowerSupply5V;
import javafx.scene.paint.Color;

import java.io.File;

/**
 * One-shot main: builds a simple "5V → resistor → LED → GND" demo circuit
 * and serializes it to demo_circuit.trc in the project root.
 *
 * Run with: ./gradlew run -PmainClass=com.trace.DemoBuilder
 * (or temporarily set application.mainClass)
 */
public class DemoBuilder {
    public static void main(String[] args) throws Exception {
        Circuit circuit = new Circuit();
        circuit.setName("Demo: lit LED");
        Breadboard bb = circuit.getBreadboard();

        // Place a 5V supply, a resistor, an LED, and a ground.
        PowerSupply5V vcc = new PowerSupply5V();
        vcc.setPosition(60, 200);

        Resistor r = new Resistor(220);
        r.setPosition(bb.getHoleX(8) - 4, bb.getHoleY(2));

        LED led = new LED(Color.RED);
        led.setPosition(bb.getHoleX(14) - 5, bb.getHoleY(2) - 30);

        Ground gnd = new Ground();
        gnd.setPosition(400, 200);

        circuit.addComponent(vcc);
        circuit.addComponent(r);
        circuit.addComponent(led);
        circuit.addComponent(gnd);

        // Wire it up using the breadboard's top + and - rails.
        // 5V → top + rail
        Wire w1 = new Wire(vcc.getPin("VCC"), bb.getTopPositive().getPoint(2).getOrCreateHolePin());
        // top + rail → resistor pin1
        Wire w2 = new Wire(bb.getTopPositive().getPoint(8).getOrCreateHolePin(), r.getPin("1"));
        // resistor pin2 → LED anode
        Wire w3 = new Wire(r.getPin("2"), led.getPin("anode"));
        // LED cathode → top - rail
        Wire w4 = new Wire(led.getPin("cathode"), bb.getTopNegative().getPoint(15).getOrCreateHolePin());
        // top - rail → GND
        Wire w5 = new Wire(bb.getTopNegative().getPoint(20).getOrCreateHolePin(), gnd.getPin("GND"));

        circuit.addWire(w1);
        circuit.addWire(w2);
        circuit.addWire(w3);
        circuit.addWire(w4);
        circuit.addWire(w5);

        File out = new File("demo_circuit.trc");
        circuit.saveToFile(out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }
}
