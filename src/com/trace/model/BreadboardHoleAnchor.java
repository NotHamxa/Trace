package com.trace.model;

import javafx.scene.canvas.GraphicsContext;

public class BreadboardHoleAnchor extends Component {
    public BreadboardHoleAnchor(int col, int letter) {
        super("Hole(" + (col + 1) + "," + (char) ('a' + letter) + ")", 0, 0);
    }

    @Override public void simulate() { }
    @Override public void render(GraphicsContext gc) { }
    @Override public Component clone() { return this; }
}
