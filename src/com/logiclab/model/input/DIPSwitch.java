package com.logiclab.model.input;

import com.logiclab.model.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class DIPSwitch extends InputComponent {
    private static final long serialVersionUID = 2L;

    // Layout constants — tuned for a larger, readable body with top-side pins.
    private static final double PITCH = 25;       // horizontal spacing between switches
    private static final double BODY_HEIGHT = 60; // taller body to fit a user tag above the slot
    private static final double SIDE_PAD = 10;    // left/right padding around the switch column
    private static final double TAG_Y_OFFSET = 18; // y offset of the tag label inside the body
    private static final double SLOT_TOP = 24;    // top of the switch slot inside the body
    private static final double SLOT_BOTTOM = 52; // bottom of the switch slot inside the body

    private boolean[] switches;
    private int numSwitches;
    private int lastToggledIndex = -1;
    private String[] tags;

    public DIPSwitch() {
        this(4);
    }

    public DIPSwitch(int numSwitches) {
        super("DIP Switch", 2 * SIDE_PAD + numSwitches * PITCH, BODY_HEIGHT);
        this.numSwitches = numSwitches;
        this.switches = new boolean[numSwitches];
        this.tags = new String[numSwitches];
        for (int i = 0; i < numSwitches; i++) {
            tags[i] = "";
            addPin(new Pin("OUT" + (i + 1), PinType.OUTPUT, this));
        }
    }

    /** Defensive accessors so tags survive deserialization of older .llb files. */
    private String[] tagsArray() {
        if (tags == null || tags.length != numSwitches) {
            tags = new String[numSwitches];
            for (int i = 0; i < numSwitches; i++) {
                if (tags[i] == null) tags[i] = "";
            }
        }
        return tags;
    }

    public String getTag(int index) {
        if (index < 0 || index >= numSwitches) return "";
        String t = tagsArray()[index];
        return t == null ? "" : t;
    }

    public void setTag(int index, String tag) {
        if (index < 0 || index >= numSwitches) return;
        tagsArray()[index] = tag == null ? "" : tag;
    }

    public int getNumSwitches() {
        return numSwitches;
    }

    @Override
    public void onInteract() {
        if (lastToggledIndex >= 0 && lastToggledIndex < numSwitches) {
            switches[lastToggledIndex] = !switches[lastToggledIndex];
        }
    }

    public void toggleSwitch(int index) {
        if (index >= 0 && index < numSwitches) {
            switches[index] = !switches[index];
        }
    }

    /** Programmatically sets one switch (used by test mode). */
    public void setSwitch(int index, boolean value) {
        if (index >= 0 && index < numSwitches) {
            switches[index] = value;
        }
    }

    /** Returns whether a specific switch is on (used by test mode). */
    public boolean isSwitchOn(int index) {
        if (index < 0 || index >= numSwitches) return false;
        return switches[index];
    }

    public void setLastToggledIndex(int index) {
        this.lastToggledIndex = index;
    }

    public int getSwitchIndexAt(double px, double py) {
        double startX = getX() + SIDE_PAD;
        double slotTop = getY() + SLOT_TOP;
        double slotBot = getY() + SLOT_BOTTOM;
        for (int i = 0; i < numSwitches; i++) {
            double sx = startX + i * PITCH;
            // Use the switch slot rectangle so the click target matches what
            // the user sees, not the empty space around the toggles.
            if (px >= sx && px <= sx + (PITCH - 5) && py >= slotTop && py <= slotBot) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Pin getOutputPin() {
        return getPin("OUT1");
    }

    @Override
    public void simulate() {
        for (int i = 0; i < numSwitches; i++) {
            Pin p = getPin("OUT" + (i + 1));
            if (p != null) {
                p.setState(switches[i] ? LogicState.HIGH : LogicState.LOW);
            }
        }
    }

    @Override
    protected void updatePinPositions() {
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        PinSide side = getPinSide();
        for (int i = 0; i < numSwitches; i++) {
            Pin p = getPin("OUT" + (i + 1));
            if (p == null) continue;
            switch (side) {
                case BOTTOM:
                    p.setPosition(x + SIDE_PAD + i * PITCH + (PITCH - 5) / 2, y + h);
                    break;
                case LEFT:
                    p.setPosition(x, y + (i + 0.5) * (h / numSwitches));
                    break;
                case RIGHT:
                    p.setPosition(x + w, y + (i + 0.5) * (h / numSwitches));
                    break;
                case TOP:
                default:
                    p.setPosition(x + SIDE_PAD + i * PITCH + (PITCH - 5) / 2, y);
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

        // Body — dark charcoal housing
        gc.setFill(Color.rgb(42, 43, 47));
        gc.fillRoundRect(x, y, w, h, 4, 4);
        gc.setStroke(Color.rgb(60, 63, 65));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, w, h, 4, 4);

        // Top pin strip recess (subtle darker band where output pins exit)
        gc.setFill(Color.rgb(23, 24, 26));
        gc.fillRect(x + 2, y + 2, w - 4, 6);

        String[] ts = tagsArray();
        gc.setTextAlign(TextAlignment.CENTER);

        for (int i = 0; i < numSwitches; i++) {
            double sx = x + SIDE_PAD + i * PITCH;
            double slotW = PITCH - 5;

            // User tag above the switch slot (inside the body)
            gc.setFont(Font.font("SansSerif", 10));
            gc.setFill(Color.rgb(223, 225, 229));
            String tag = ts[i] == null ? "" : ts[i];
            if (!tag.isEmpty()) {
                gc.fillText(tag, sx + slotW / 2, y + TAG_Y_OFFSET);
            }

            // Switch slot well
            double slotH = SLOT_BOTTOM - SLOT_TOP;
            gc.setFill(Color.rgb(15, 16, 18));
            gc.fillRoundRect(sx, y + SLOT_TOP, slotW, slotH, 2, 2);
            gc.setStroke(Color.rgb(60, 63, 65));
            gc.setLineWidth(0.8);
            gc.strokeRoundRect(sx, y + SLOT_TOP, slotW, slotH, 2, 2);

            // Switch toggle — amber when ON (classic DIP look), muted gray when OFF
            double toggleH = slotH / 2 - 2;
            double mid = y + (SLOT_TOP + SLOT_BOTTOM) / 2;
            double toggleY = switches[i] ? y + SLOT_TOP + 2 : mid;
            gc.setFill(switches[i] ? Color.rgb(232, 163, 61) : Color.rgb(95, 98, 103));
            gc.fillRoundRect(sx + 2, toggleY, slotW - 4, toggleH, 1, 1);
            // Thin highlight line across the top of the toggle
            gc.setStroke(switches[i] ? Color.rgb(255, 200, 120, 0.7) : Color.rgb(160, 163, 168, 0.5));
            gc.setLineWidth(0.6);
            gc.strokeLine(sx + 3, toggleY + 1, sx + slotW - 3, toggleY + 1);

            // Switch number below the body
            gc.setFont(Font.font("Monospaced", 9));
            gc.setFill(Color.rgb(134, 138, 145));
            gc.fillText(String.valueOf(i + 1), sx + slotW / 2, y + h + 11);
        }

        // Output pins (rendered last so they draw on top of the body edge)
        for (int i = 0; i < numSwitches; i++) {
            getPin("OUT" + (i + 1)).render(gc);
        }
    }

    @Override
    public Component clone() {
        DIPSwitch copy = new DIPSwitch(numSwitches);
        String[] ts = tagsArray();
        for (int i = 0; i < numSwitches; i++) {
            copy.setTag(i, ts[i]);
        }
        return copy;
    }
}
