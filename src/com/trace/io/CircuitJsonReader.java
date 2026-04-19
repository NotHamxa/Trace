package com.trace.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trace.io.dto.BreadboardDTO;
import com.trace.io.dto.CircuitDTO;
import com.trace.io.dto.ComponentDTO;
import com.trace.io.dto.PinRefDTO;
import com.trace.io.dto.TestCasesDTO;
import com.trace.io.dto.WireDTO;
import com.trace.model.Breadboard;
import com.trace.model.Circuit;
import com.trace.model.Component;
import com.trace.model.ContactPoint;
import com.trace.model.LogicState;
import com.trace.model.Pin;
import com.trace.model.PowerRail;
import com.trace.model.Wire;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Parses the JSON-on-disk format into a live Circuit. */
public final class CircuitJsonReader {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private CircuitJsonReader() {}

    public static Circuit read(File file) throws IOException {
        CircuitDTO dto = MAPPER.readValue(file, CircuitDTO.class);
        return fromDto(dto);
    }

    public static Circuit readFromString(String json) throws IOException {
        return fromDto(MAPPER.readValue(json, CircuitDTO.class));
    }

    public static Circuit fromDto(CircuitDTO dto) {
        Circuit circuit = new Circuit();
        if (dto.name() != null) circuit.setName(dto.name());

        // Breadboards
        List<Breadboard> bbs = new ArrayList<>();
        if (dto.breadboards() != null) {
            // Preserve saved order by index
            BreadboardDTO[] ordered = new BreadboardDTO[dto.breadboards().size()];
            for (BreadboardDTO b : dto.breadboards()) {
                int i = Math.max(0, Math.min(ordered.length - 1, b.index()));
                ordered[i] = b;
            }
            for (BreadboardDTO b : ordered) {
                if (b == null) continue;
                Breadboard bb = new Breadboard(b.columns());
                bb.setPosition(b.x(), b.y());
                bbs.add(bb);
            }
        }
        if (bbs.isEmpty()) bbs.add(new Breadboard(30));
        circuit.replaceBreadboards(bbs);

        // Components
        Map<String, Component> byId = new HashMap<>();
        if (dto.components() != null) {
            for (ComponentDTO cd : dto.components()) {
                Component c = ComponentFactory.create(cd);
                circuit.addComponent(c);
                byId.put(c.getId(), c);
            }
        }

        // Wires
        if (dto.wires() != null) {
            for (WireDTO wd : dto.wires()) {
                Pin start = resolve(wd.start(), bbs, byId);
                Pin end = resolve(wd.end(), bbs, byId);
                if (start == null || end == null) continue;
                Wire w = new Wire(start, end);
                if (wd.colorHex() != null) {
                    try { w.setColor(Color.web(wd.colorHex())); } catch (Exception ignore) {}
                }
                w.setLocked(wd.locked());
                if (wd.waypoints() != null) {
                    w.getWaypoints().clear();
                    for (double[] wp : wd.waypoints()) {
                        if (wp != null && wp.length >= 2) {
                            w.getWaypoints().add(new double[]{wp[0], wp[1]});
                        }
                    }
                }
                circuit.addWire(w);
            }
        }

        // Tests
        TestCasesDTO t = dto.tests();
        if (t != null && t.inputs() != null && t.expected() != null) {
            circuit.setSavedTests(toStateRows(t.inputs()), toStateRows(t.expected()));
        }

        circuit.setModified(false);
        return circuit;
    }

    private static Pin resolve(PinRefDTO ref, List<Breadboard> bbs, Map<String, Component> byId) {
        if (ref == null || ref.kind() == null) return null;
        switch (ref.kind()) {
            case "component": {
                Component c = byId.get(ref.componentId());
                if (c == null) return null;
                return c.getPin(ref.pinLabel());
            }
            case "hole": {
                Breadboard bb = bbAt(bbs, ref.breadboardIndex());
                if (bb == null) return null;
                ContactPoint cp = bb.getHole(nonNull(ref.col()), nonNull(ref.row()));
                return cp == null ? null : cp.getOrCreateHolePin();
            }
            case "rail": {
                Breadboard bb = bbAt(bbs, ref.breadboardIndex());
                if (bb == null) return null;
                PowerRail rail = switch (ref.rail() == null ? "" : ref.rail()) {
                    case "TOP_PLUS"   -> bb.getTopPositive();
                    case "TOP_MINUS"  -> bb.getTopNegative();
                    case "BOT_PLUS"   -> bb.getBottomPositive();
                    case "BOT_MINUS"  -> bb.getBottomNegative();
                    default -> null;
                };
                if (rail == null) return null;
                ContactPoint cp = rail.getPoint(nonNull(ref.col()));
                return cp == null ? null : cp.getOrCreateHolePin();
            }
            default:
                return null;
        }
    }

    private static Breadboard bbAt(List<Breadboard> bbs, Integer idx) {
        if (idx == null || idx < 0 || idx >= bbs.size()) return null;
        return bbs.get(idx);
    }

    private static int nonNull(Integer v) { return v == null ? 0 : v; }

    private static List<LogicState[]> toStateRows(List<List<String>> rows) {
        List<LogicState[]> out = new ArrayList<>();
        for (List<String> row : rows) {
            LogicState[] arr = new LogicState[row.size()];
            for (int i = 0; i < row.size(); i++) {
                String s = row.get(i);
                arr[i] = (s == null) ? null : LogicState.valueOf(s);
            }
            out.add(arr);
        }
        return out;
    }
}
