package com.trace.model.output;

import com.trace.model.Component;
import com.trace.model.LogicState;
import com.trace.model.OutputComponent;
import com.trace.model.Pin;
import com.trace.model.PinSide;
import com.trace.model.PinType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * A horizontal strip of 8 indicator lights, one per input pin.
 * HIGH → red, LOW → green, FLOATING → dim/off.
 */
public class LightBar extends OutputComponent {
    private static final int COUNT = 8;
    private static final double SPACING = 20;
    private static final double LIGHT_RADIUS = 6;
    private static final double H_WIDTH  = 10 + SPACING * COUNT;   // horizontal layout
    private static final double H_HEIGHT = 50;
    private static final double V_WIDTH  = 50;                     // vertical layout
    private static final double V_HEIGHT = 10 + SPACING * COUNT;

    private PinSide pinSide = PinSide.BOTTOM;
    private String[] tags;

    public LightBar() {
        super("Light Bar", H_WIDTH, H_HEIGHT);
        tags = new String[COUNT];
        for (int i = 0; i < COUNT; i++) {
            tags[i] = "";
            addPin(new Pin("IN" + i, PinType.INPUT, this));
        }
    }

    /** Null-safe so older serialized circuits (field missing) default to BOTTOM. */
    public PinSide getPinSide() {
        return pinSide == null ? PinSide.BOTTOM : pinSide;
    }

    public void setPinSide(PinSide side) {
        this.pinSide = side == null ? PinSide.BOTTOM : side;
        boolean vertical = this.pinSide == PinSide.LEFT || this.pinSide == PinSide.RIGHT;
        setSize(vertical ? V_WIDTH : H_WIDTH, vertical ? V_HEIGHT : H_HEIGHT);
        updatePinPositions();
    }

    private boolean isVertical() {
        PinSide s = getPinSide();
        return s == PinSide.LEFT || s == PinSide.RIGHT;
    }

    private String[] tagsArray() {
        if (tags == null || tags.length != COUNT) {
            tags = new String[COUNT];
            for (int i = 0; i < COUNT; i++) {
                if (tags[i] == null) tags[i] = "";
            }
        }
        return tags;
    }

    public String getTag(int index) {
        if (index < 0 || index >= COUNT) return "";
        String t = tagsArray()[index];
        return t == null ? "" : t;
    }

    public void setTag(int index, String tag) {
        if (index < 0 || index >= COUNT) return;
        tagsArray()[index] = tag == null ? "" : tag;
    }

    public int getLightCount() {
        return COUNT;
    }

    @Override
    public void simulate() {
        // Purely visual — nothing to drive.
    }

    @Override
    public boolean isActive() {
        for (int i = 0; i < COUNT; i++) {
            if (getPin("IN" + i).getState() != LogicState.FLOATING) return true;
        }
        return false;
    }

    @Override
    protected void updatePinPositions() {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        PinSide side = getPinSide();
        for (int i = 0; i < COUNT; i++) {
            Pin p = getPin("IN" + i);
            if (p == null) continue;
            switch (side) {
                case TOP:
                    p.setPosition(x + 15 + i * SPACING, y);
                    break;
                case LEFT:
                    p.setPosition(x, y + 15 + i * SPACING);
                    break;
                case RIGHT:
                    p.setPosition(x + w, y + 15 + i * SPACING);
                    break;
                case BOTTOM:
                default:
                    p.setPosition(x + 15 + i * SPACING, y + h);
                    break;
            }
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        boolean vert = isVertical();

        // Body dimensions (leave room for pin stubs)
        double bodyW = vert ? w - 10 : w;
        double bodyH = vert ? h : h - 10;
        double bodyX = getPinSide() == PinSide.LEFT ? x + 10 : x;
        double bodyY = getPinSide() == PinSide.TOP ? y + 10 : y;

        // Housing
        gc.setFill(Color.rgb(22, 22, 24));
        gc.fillRoundRect(bodyX, bodyY, bodyW, bodyH, 5, 5);
        gc.setStroke(Color.rgb(80, 80, 85));
        gc.setLineWidth(1);
        gc.strokeRoundRect(bodyX, bodyY, bodyW, bodyH, 5, 5);

        String[] ts = tagsArray();

        for (int i = 0; i < COUNT; i++) {
            double cx, cy;
            if (vert) {
                cx = bodyX + bodyW / 2;
                cy = y + 15 + i * SPACING;
            } else {
                cx = x + 15 + i * SPACING;
                cy = bodyY + bodyH / 2 - 5;
            }
            LogicState state = getPin("IN" + i).getState();
            Color fill;
            boolean lit;
            switch (state) {
                case HIGH:
                    fill = Color.rgb(235, 60, 60);
                    lit = true;
                    break;
                case LOW:
                    fill = Color.rgb(60, 210, 90);
                    lit = true;
                    break;
                default:
                    fill = Color.rgb(40, 40, 42);
                    lit = false;
                    break;
            }
            // Glow halo when lit
            if (lit) {
                gc.setFill(fill.deriveColor(0, 1, 1, 0.28));
                double r = LIGHT_RADIUS * 1.9;
                gc.fillOval(cx - r, cy - r, r * 2, r * 2);
            }
            // Light body
            gc.setFill(fill);
            gc.fillOval(cx - LIGHT_RADIUS, cy - LIGHT_RADIUS, LIGHT_RADIUS * 2, LIGHT_RADIUS * 2);
            gc.setStroke(Color.rgb(70, 70, 75));
            gc.setLineWidth(0.8);
            gc.strokeOval(cx - LIGHT_RADIUS, cy - LIGHT_RADIUS, LIGHT_RADIUS * 2, LIGHT_RADIUS * 2);

            // Highlight dot
            if (lit) {
                gc.setFill(Color.rgb(255, 255, 255, 0.35));
                gc.fillOval(cx - LIGHT_RADIUS * 0.55, cy - LIGHT_RADIUS * 0.55,
                        LIGHT_RADIUS * 0.6, LIGHT_RADIUS * 0.6);
            }

            // Tag label
            String tag = ts[i];
            if (tag != null && !tag.isEmpty()) {
                gc.setFont(Font.font("SansSerif", 9));
                gc.setFill(Color.rgb(180, 182, 186));
                if (vert) {
                    gc.setTextAlign(TextAlignment.LEFT);
                    gc.fillText(tag, cx + LIGHT_RADIUS + 4, cy + 3);
                } else {
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText(tag, cx, cy + LIGHT_RADIUS + 12);
                }
            }
        }

        // Render pins
        for (Pin p : getPins()) {
            p.render(gc);
        }
    }

    @Override
    public Component clone() {
        LightBar copy = new LightBar();
        copy.setPinSide(getPinSide());
        String[] ts = tagsArray();
        for (int i = 0; i < COUNT; i++) {
            copy.setTag(i, ts[i]);
        }
        return copy;
    }
}
