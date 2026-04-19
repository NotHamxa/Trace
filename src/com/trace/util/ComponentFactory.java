package com.trace.util;

import com.trace.model.Component;
import com.trace.model.chips.*;
import com.trace.model.input.*;
import com.trace.model.output.*;
import com.trace.model.passive.*;
import com.trace.model.power.*;
import javafx.scene.paint.Color;

public class ComponentFactory {

    public static Component create(String type) {
        switch (type.toUpperCase()) {
            case "AND":
            case "7408":
                return new ANDGate7408();
            case "OR":
            case "7432":
                return new ORGate7432();
            case "NOT":
            case "7404":
                return new NOTGate7404();
            case "NAND":
            case "7400":
                return new NANDGate7400();
            case "NOR":
            case "7402":
                return new NORGate7402();
            case "XOR":
            case "7486":
                return new XORGate7486();
            case "TOGGLE":
            case "TOGGLE_SWITCH":
                return new ToggleSwitch();
            case "BUTTON":
            case "PUSH_BUTTON":
                return new PushButton();
            case "DIP":
            case "DIP_SWITCH":
                return new DIPSwitch();
            case "LED_RED":
                return new LED(Color.RED);
            case "LED_GREEN":
                return new LED(Color.GREEN);
            case "LED_YELLOW":
                return new LED(Color.YELLOW);
            case "7SEG":
            case "SEVEN_SEGMENT":
                return new SevenSegmentDisplay();
            case "RESISTOR":
                return new Resistor();
            case "5V":
            case "POWER":
                return new PowerSupply5V();
            case "GND":
            case "GROUND":
                return new Ground();
            default:
                throw new IllegalArgumentException("Unknown component type: " + type);
        }
    }
}
