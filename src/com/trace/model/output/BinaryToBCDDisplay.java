package com.trace.model.output;

import com.trace.model.Component;
import com.trace.model.LogicState;
import com.trace.model.OutputComponent;
import com.trace.model.Pin;
import com.trace.model.PinType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class BinaryToBCDDisplay extends OutputComponent {
    private static final String[] INPUTS = {"B3", "B2", "B1", "B0"};

    private static final boolean[][] DIGITS = {
        {true,  true,  true,  true,  true,  true,  false}, // 0
        {false, true,  true,  false, false, false, false}, // 1
        {true,  true,  false, true,  true,  false, true},  // 2
        {true,  true,  true,  true,  false, false, true},  // 3
        {false, true,  true,  false, false, true,  true},  // 4
        {true,  false, true,  true,  false, true,  true},  // 5
        {true,  false, true,  true,  true,  true,  true},  // 6
        {true,  true,  true,  false, false, false, false}, // 7
        {true,  true,  true,  true,  true,  true,  true},  // 8
        {true,  true,  true,  true,  false, true,  true},  // 9
    };
    private static final boolean[] ERROR_E =
        {true, false, false, true, true, true, true}; // "E"

    public BinaryToBCDDisplay() {
        super("Binary\u2192BCD", 70, 100);
        for (String p : INPUTS) {
            addPin(new Pin(p, PinType.INPUT, this));
        }
    }

    @Override
    public void simulate() {
    }

    @Override
    public boolean isActive() {
        for (String p : INPUTS) {
            if (getPin(p).getState() != LogicState.FLOATING) return true;
        }
        return false;
    }

    private boolean[] currentSegments() {
        int value = 0;
        for (int bit = 0; bit < 4; bit++) {
            LogicState s = getPin("B" + bit).getState();
            if (s == LogicState.FLOATING) return null; // no valid input → blank
            if (s == LogicState.HIGH) value |= (1 << bit);
        }
        if (value >= 0 && value <= 9) return DIGITS[value];
        return ERROR_E;
    }

    @Override
    protected void updatePinPositions() {
        double x = getX();
        double y = getY();
        double pitch = 15;
        double start = getWidth() / 2 - pitch * 1.5;
        for (int i = 0; i < INPUTS.length; i++) {
            getPin(INPUTS[i]).setPosition(x + start + i * pitch, y + getHeight());
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double bodyH = getHeight() - 10;

        gc.setFill(Color.rgb(18, 18, 20));
        gc.fillRoundRect(x, y, w, bodyH, 5, 5);
        gc.setStroke(Color.rgb(80, 80, 85));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, w, bodyH, 5, 5);

        boolean[] segs = currentSegments();
        Color on = Color.rgb(235, 55, 55);
        Color off = Color.rgb(55, 20, 20);

        double segLen = 28;
        double segThk = 5;
        double cx = x + w / 2;
        double cyMid = y + bodyH / 2;
        double top = y + 14;
        double bot = y + bodyH - 14;
        double vH = (bot - top) / 2 - segThk / 2;

        gc.setFill(segOn(segs, 0) ? on : off);
        gc.fillRect(cx - segLen / 2, top, segLen, segThk);
        gc.setFill(segOn(segs, 1) ? on : off);
        gc.fillRect(cx + segLen / 2 - segThk, top + segThk, segThk, vH);
        gc.setFill(segOn(segs, 2) ? on : off);
        gc.fillRect(cx + segLen / 2 - segThk, cyMid + segThk / 2, segThk, vH);
        gc.setFill(segOn(segs, 3) ? on : off);
        gc.fillRect(cx - segLen / 2, bot - segThk, segLen, segThk);
        gc.setFill(segOn(segs, 4) ? on : off);
        gc.fillRect(cx - segLen / 2, cyMid + segThk / 2, segThk, vH);
        gc.setFill(segOn(segs, 5) ? on : off);
        gc.fillRect(cx - segLen / 2, top + segThk, segThk, vH);
        gc.setFill(segOn(segs, 6) ? on : off);
        gc.fillRect(cx - segLen / 2, cyMid - segThk / 2, segLen, segThk);

        for (Pin p : getPins()) {
            p.render(gc);
        }
    }

    private boolean segOn(boolean[] segs, int idx) {
        return segs != null && segs[idx];
    }

    @Override
    public Component clone() {
        return new BinaryToBCDDisplay();
    }
}
