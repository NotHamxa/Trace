package com.trace.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import com.trace.model.HolePin;
import com.trace.model.LogicState;
import com.trace.model.Pin;
import com.trace.model.PowerRail;
import com.trace.model.Wire;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Serializes a Circuit to the JSON-on-disk format. */
public final class CircuitJsonWriter {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private CircuitJsonWriter() {}

    public static void write(Circuit circuit, File file) throws IOException {
        CircuitDTO dto = toDto(circuit);
        MAPPER.writeValue(file, dto);
    }

    public static String writeToString(Circuit circuit) throws IOException {
        return MAPPER.writeValueAsString(toDto(circuit));
    }

    public static CircuitDTO toDto(Circuit circuit) {
        List<Breadboard> bbs = circuit.getBreadboards();

        List<BreadboardDTO> bbDtos = new ArrayList<>();
        for (int i = 0; i < bbs.size(); i++) {
            Breadboard bb = bbs.get(i);
            bbDtos.add(new BreadboardDTO(i, bb.getRows(), bb.getBoardX(), bb.getBoardY()));
        }

        List<ComponentDTO> compDtos = new ArrayList<>();
        for (Component c : circuit.getComponents()) {
            compDtos.add(new ComponentDTO(
                    c.getId(),
                    ComponentFactory.typeOf(c),
                    c.getX(),
                    c.getY(),
                    c.getDisplayLabel(),
                    c.isLocked(),
                    ComponentFactory.extractProps(c)
            ));
        }

        List<WireDTO> wireDtos = new ArrayList<>();
        for (Wire w : circuit.getWires()) {
            PinRefDTO start = refOf(w.getStartPin(), bbs);
            PinRefDTO end = refOf(w.getEndPin(), bbs);
            if (start == null || end == null) continue; // unresolvable — skip
            wireDtos.add(new WireDTO(
                    start, end,
                    toHex(w.getColor()),
                    w.isLocked(),
                    new ArrayList<>(w.getWaypoints())
            ));
        }

        TestCasesDTO tests = null;
        if (circuit.getSavedTestInputs() != null && circuit.getSavedTestExpected() != null) {
            tests = new TestCasesDTO(
                    toStringRows(circuit.getSavedTestInputs()),
                    toStringRows(circuit.getSavedTestExpected())
            );
        }

        return new CircuitDTO(
                CircuitDTO.CURRENT_VERSION,
                circuit.getName(),
                bbDtos, compDtos, wireDtos,
                tests
        );
    }

    private static PinRefDTO refOf(Pin pin, List<Breadboard> bbs) {
        if (pin instanceof HolePin hp) {
            ContactPoint cp = hp.getContact();
            for (int i = 0; i < bbs.size(); i++) {
                Breadboard bb = bbs.get(i);
                PinRefDTO ref = refForContact(bb, i, cp);
                if (ref != null) return ref;
            }
            return null; // orphaned HolePin
        }
        Component owner = pin.getOwner();
        if (owner == null) return null;
        return PinRefDTO.component(owner.getId(), pin.getLabel());
    }

    private static PinRefDTO refForContact(Breadboard bb, int bbIndex, ContactPoint cp) {
        // Check main grid: grid points expose (row=col, col=letter) — so search by identity.
        int cols = bb.getRows();
        for (int col = 0; col < cols; col++) {
            for (int letter = 0; letter < 10; letter++) {
                if (bb.getHole(col, letter) == cp) {
                    return PinRefDTO.hole(bbIndex, col, letter);
                }
            }
        }
        // Check the 4 power rails.
        PinRefDTO r = findOnRail(bb.getTopPositive(), bbIndex, "TOP_PLUS", cp);
        if (r != null) return r;
        r = findOnRail(bb.getTopNegative(), bbIndex, "TOP_MINUS", cp);
        if (r != null) return r;
        r = findOnRail(bb.getBottomPositive(), bbIndex, "BOT_PLUS", cp);
        if (r != null) return r;
        r = findOnRail(bb.getBottomNegative(), bbIndex, "BOT_MINUS", cp);
        return r;
    }

    private static PinRefDTO findOnRail(PowerRail rail, int bbIndex, String name, ContactPoint cp) {
        List<ContactPoint> pts = rail.getPoints();
        for (int i = 0; i < pts.size(); i++) {
            if (pts.get(i) == cp) return PinRefDTO.rail(bbIndex, name, i);
        }
        return null;
    }

    private static String toHex(javafx.scene.paint.Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static List<List<String>> toStringRows(List<LogicState[]> rows) {
        List<List<String>> out = new ArrayList<>();
        for (LogicState[] row : rows) {
            List<String> r = new ArrayList<>(row.length);
            for (LogicState s : row) r.add(s == null ? null : s.name());
            out.add(r);
        }
        return out;
    }
}
