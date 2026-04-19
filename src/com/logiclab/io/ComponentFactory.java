package com.logiclab.io;

import com.logiclab.io.dto.ComponentDTO;
import com.logiclab.model.Component;
import com.logiclab.model.PinSide;
import com.logiclab.model.chips.*;
import com.logiclab.model.flipflops.SRFlipFlop;
import com.logiclab.model.input.DIPSwitch;
import com.logiclab.model.input.PushButton;
import com.logiclab.model.input.ToggleSwitch;
import com.logiclab.model.output.BinaryToBCDDisplay;
import com.logiclab.model.output.LED;
import com.logiclab.model.output.LightBar;
import com.logiclab.model.output.SevenSegmentDisplay;
import com.logiclab.model.passive.Resistor;
import com.logiclab.model.ports.InputPort;
import com.logiclab.model.ports.OutputPort;
import com.logiclab.model.power.Ground;
import com.logiclab.model.power.PowerSupply5V;
import com.logiclab.model.subcircuit.SubCircuitDefinition;
import com.logiclab.model.subcircuit.SubCircuitInstance;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps concrete Component classes to stable type-string identifiers used in
 * the JSON save file, and handles per-type prop extraction / application.
 *
 * Renaming a component class doesn't break saved files as long as its type
 * string here stays the same.
 */
public final class ComponentFactory {
    private ComponentFactory() {}

    public static String typeOf(Component c) {
        if (c instanceof ANDGate7408)          return "IC_7408";
        if (c instanceof ORGate7432)           return "IC_7432";
        if (c instanceof XORGate7486)          return "IC_7486";
        if (c instanceof NANDGate7400)         return "IC_7400";
        if (c instanceof NORGate7402)          return "IC_7402";
        if (c instanceof NOTGate7404)          return "IC_7404";
        if (c instanceof ANDGate7411)          return "IC_7411";
        if (c instanceof NANDGate7410)         return "IC_7410";
        if (c instanceof NORGate7427)          return "IC_7427";
        if (c instanceof FourBitAdder7483)     return "IC_7483";
        if (c instanceof SRFlipFlop)           return "FF_SR";
        if (c instanceof DIPSwitch)            return "DIPSWITCH";
        if (c instanceof ToggleSwitch)         return "TOGGLE";
        if (c instanceof PushButton)           return "PUSHBUTTON";
        if (c instanceof LED)                  return "LED";
        if (c instanceof LightBar)             return "LIGHTBAR";
        if (c instanceof SevenSegmentDisplay)  return "SEVENSEG";
        if (c instanceof BinaryToBCDDisplay)   return "BCD_DISPLAY";
        if (c instanceof Resistor)             return "RESISTOR";
        if (c instanceof PowerSupply5V)        return "POWER_5V";
        if (c instanceof Ground)               return "GND";
        if (c instanceof InputPort)            return "INPUT_PORT";
        if (c instanceof OutputPort)           return "OUTPUT_PORT";
        if (c instanceof SubCircuitInstance)   return "SUBCIRCUIT";
        throw new IllegalArgumentException("Unknown component class: " + c.getClass().getName());
    }

    /** Extracts per-type state (the stuff not captured by id/pos/label/locked). */
    public static Map<String, Object> extractProps(Component c) {
        Map<String, Object> props = new HashMap<>();
        if (c instanceof DIPSwitch d) {
            int n = d.getNumSwitches();
            props.put("numSwitches", n);
            boolean[] states = new boolean[n];
            String[] tags = new String[n];
            for (int i = 0; i < n; i++) {
                states[i] = d.isSwitchOn(i);
                tags[i] = d.getTag(i);
            }
            props.put("states", states);
            props.put("tags", tags);
            props.put("pinSide", d.getPinSide().name());
        } else if (c instanceof ToggleSwitch t) {
            props.put("on", t.isOn());
            props.put("pinSide", t.getPinSide().name());
        } else if (c instanceof PushButton p) {
            props.put("pinSide", p.getPinSide().name());
        } else if (c instanceof LED l) {
            Color col = l.getColor();
            props.put("colorHex", hex(col));
        } else if (c instanceof LightBar lb) {
            props.put("pinSide", lb.getPinSide().name());
            int count = lb.getLightCount();
            String[] tags = new String[count];
            for (int i = 0; i < count; i++) tags[i] = lb.getTag(i);
            props.put("tags", tags);
        } else if (c instanceof Resistor r) {
            props.put("resistance", r.getResistance());
        } else if (c instanceof InputPort ip) {
            props.put("label", ip.getPortLabel());
        } else if (c instanceof OutputPort op) {
            props.put("label", op.getPortLabel());
        } else if (c instanceof SubCircuitInstance sci) {
            props.put("ref", sci.getRef());
        }
        return props;
    }

    /** Creates a fresh component from a DTO and applies position / id / label / locked / props. */
    public static Component create(ComponentDTO dto) {
        Component c = instantiate(dto.type(), dto.props());
        c.setId(dto.id());
        c.setPosition(dto.x(), dto.y());
        if (dto.displayLabel() != null) c.setDisplayLabel(dto.displayLabel());
        c.setLocked(dto.locked());
        c.setPlaced(true);
        applyProps(c, dto.props());
        c.setPosition(c.getX(), c.getY()); // re-run pin layout after prop-driven size changes
        return c;
    }

    private static Component instantiate(String type, Map<String, Object> props) {
        return switch (type) {
            case "IC_7408"      -> new ANDGate7408();
            case "IC_7432"      -> new ORGate7432();
            case "IC_7486"      -> new XORGate7486();
            case "IC_7400"      -> new NANDGate7400();
            case "IC_7402"      -> new NORGate7402();
            case "IC_7404"      -> new NOTGate7404();
            case "IC_7411"      -> new ANDGate7411();
            case "IC_7410"      -> new NANDGate7410();
            case "IC_7427"      -> new NORGate7427();
            case "IC_7483"      -> new FourBitAdder7483();
            case "FF_SR"        -> new SRFlipFlop();
            case "DIPSWITCH"    -> new DIPSwitch(asInt(props, "numSwitches", 4));
            case "TOGGLE"       -> new ToggleSwitch();
            case "PUSHBUTTON"   -> new PushButton();
            case "LED"          -> new LED(parseColor((String) props.getOrDefault("colorHex", "#FF0000")));
            case "LIGHTBAR"     -> new LightBar();
            case "SEVENSEG"     -> new SevenSegmentDisplay();
            case "BCD_DISPLAY"  -> new BinaryToBCDDisplay();
            case "RESISTOR"     -> new Resistor(asInt(props, "resistance", 220));
            case "POWER_5V"     -> new PowerSupply5V();
            case "GND"          -> new Ground();
            case "INPUT_PORT"   -> new InputPort(asString(props, "label", "IN"));
            case "OUTPUT_PORT"  -> new OutputPort(asString(props, "label", "OUT"));
            case "SUBCIRCUIT"   -> {
                String ref = asString(props, "ref", "");
                SubCircuitDefinition def = SubCircuitLibrary.get(ref);
                yield new SubCircuitInstance(ref, def);
            }
            default -> throw new IllegalArgumentException("Unknown component type: " + type);
        };
    }

    private static void applyProps(Component c, Map<String, Object> props) {
        if (props == null) return;
        if (c instanceof DIPSwitch d) {
            Object states = props.get("states");
            if (states instanceof List<?> list) {
                for (int i = 0; i < list.size() && i < d.getNumSwitches(); i++) {
                    d.setSwitch(i, Boolean.TRUE.equals(list.get(i)));
                }
            }
            Object tags = props.get("tags");
            if (tags instanceof List<?> tl) {
                for (int i = 0; i < tl.size() && i < d.getNumSwitches(); i++) {
                    d.setTag(i, tl.get(i) == null ? "" : tl.get(i).toString());
                }
            }
            Object side = props.get("pinSide");
            if (side != null) d.setPinSide(parsePinSide(side.toString()));
        } else if (c instanceof ToggleSwitch t) {
            Object on = props.get("on");
            if (on instanceof Boolean b) t.setState(b);
            Object side = props.get("pinSide");
            if (side != null) t.setPinSide(parsePinSide(side.toString()));
        } else if (c instanceof PushButton p) {
            Object side = props.get("pinSide");
            if (side != null) p.setPinSide(parsePinSide(side.toString()));
        } else if (c instanceof LightBar lb) {
            Object side = props.get("pinSide");
            if (side != null) lb.setPinSide(parsePinSide(side.toString()));
            Object tags = props.get("tags");
            if (tags instanceof List<?> tl) {
                for (int i = 0; i < tl.size() && i < lb.getLightCount(); i++) {
                    lb.setTag(i, tl.get(i) == null ? "" : tl.get(i).toString());
                }
            }
        } else if (c instanceof Resistor r) {
            r.setResistance(asInt(props, "resistance", r.getResistance()));
        } else if (c instanceof InputPort ip) {
            Object label = props.get("label");
            if (label != null) ip.setPortLabel(label.toString());
        } else if (c instanceof OutputPort op) {
            Object label = props.get("label");
            if (label != null) op.setPortLabel(label.toString());
        }
        // SubCircuitInstance is configured at instantiate() via the library lookup;
        // nothing additional to apply here.
    }

    private static String asString(Map<String, Object> props, String key, String fallback) {
        if (props == null) return fallback;
        Object v = props.get(key);
        return v == null ? fallback : v.toString();
    }

    private static int asInt(Map<String, Object> props, String key, int fallback) {
        if (props == null) return fallback;
        Object v = props.get(key);
        if (v instanceof Number n) return n.intValue();
        return fallback;
    }

    private static PinSide parsePinSide(String name) {
        try {
            return PinSide.valueOf(name);
        } catch (Exception e) {
            return PinSide.TOP;
        }
    }

    private static String hex(Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static Color parseColor(String hex) {
        try {
            return Color.web(hex);
        } catch (Exception e) {
            return Color.RED;
        }
    }
}
