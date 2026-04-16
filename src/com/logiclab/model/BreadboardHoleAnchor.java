package com.logiclab.model;

import javafx.scene.canvas.GraphicsContext;

/**
 * Invisible stub Component used as the owner of a HolePin.
 * One instance per ContactPoint, so wires between two different bare holes
 * pass the "different owners" check in CanvasView.finishWire.
 */
public class BreadboardHoleAnchor extends Component {
    public BreadboardHoleAnchor(int col, int letter) {
        super("Hole(" + (col + 1) + "," + (char) ('a' + letter) + ")", 0, 0);
    }

    @Override public void simulate() { }
    @Override public void render(GraphicsContext gc) { }
    @Override public Component clone() { return this; }
}
