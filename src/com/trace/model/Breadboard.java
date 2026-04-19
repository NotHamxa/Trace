package com.trace.model;

import com.trace.interfaces.Renderable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal solderless breadboard.
 *  - "rows" is the long axis (horizontal column count, e.g. 30 columns numbered 1..N).
 *  - There are 10 lettered hole-rows (a-j) split into two banks of 5 by a center gap.
 *  - Two power rails (+/-) sit above and below the main grid.
 */
public class Breadboard implements Serializable, Renderable {
    private int rows;            // long axis: number of numbered columns
    private int cols;            // short axis: 10 lettered rows (a-j)
    private ContactPoint[][] grid; // grid[col][letter]
    private PowerRail topPositive, topNegative;
    private PowerRail bottomPositive, bottomNegative;
    private List<Net> allNets;

    // Layout constants — all multiples of HOLE_SPACING so the world grid aligns
    public static final double HOLE_SPACING = 20;
    public static final double BOARD_PADDING = 40;
    public static final double GAP_HEIGHT = 20;   // vertical gap between a-e and f-j banks
    public static final double RAIL_GAP = 20;     // gap between power rails and main grid
    public static final double HOLE_RADIUS = 3;

    private double boardX, boardY;

    public Breadboard(int rows) {
        this.rows = rows;
        this.cols = 10;
        this.grid = new ContactPoint[rows][cols];
        this.allNets = new ArrayList<>();
        initializeConnections();
        this.boardX = BOARD_PADDING;
        this.boardY = BOARD_PADDING;
    }

    public void setPosition(double x, double y) {
        this.boardX = x;
        this.boardY = y;
    }

    public double getBoardX() { return boardX; }
    public double getBoardY() { return boardY; }

    /** Returns true if the given world coordinate is on the border/outskirts of this breadboard. */
    public boolean containsPointOnBorder(double x, double y) {
        double x0 = boardX - 20;
        double y0 = boardY - 20;
        double w = getBoardWidth();
        double h = getBoardHeight();
        double margin = 15; // how thick the draggable border strip is

        // Must be inside outer bounds
        if (x < x0 || x > x0 + w || y < y0 || y > y0 + h) return false;

        // Must be outside inner area (i.e. on the border strip)
        boolean insideInner = x > x0 + margin && x < x0 + w - margin
                           && y > y0 + margin && y < y0 + h - margin;
        return !insideInner;
    }

    /** Returns true if the given world coordinate is anywhere inside this breadboard's bounding box. */
    public boolean containsPoint(double x, double y) {
        double x0 = boardX - 20;
        double y0 = boardY - 20;
        return x >= x0 && x <= x0 + getBoardWidth() && y >= y0 && y <= y0 + getBoardHeight();
    }

    private void initializeConnections() {
        for (int col = 0; col < rows; col++) {
            // Each numbered column has two independent nets (a-e and f-j).
            Net topNet = new Net();
            allNets.add(topNet);
            for (int letter = 0; letter < 5; letter++) {
                grid[col][letter] = new ContactPoint(col, letter, topNet);
            }

            Net bottomNet = new Net();
            allNets.add(bottomNet);
            for (int letter = 5; letter < 10; letter++) {
                grid[col][letter] = new ContactPoint(col, letter, bottomNet);
            }
        }

        topPositive = new PowerRail(PowerRail.RailType.POSITIVE, rows);
        topNegative = new PowerRail(PowerRail.RailType.NEGATIVE, rows);
        bottomPositive = new PowerRail(PowerRail.RailType.POSITIVE, rows);
        bottomNegative = new PowerRail(PowerRail.RailType.NEGATIVE, rows);

        allNets.add(topPositive.getNet());
        allNets.add(topNegative.getNet());
        allNets.add(bottomPositive.getNet());
        allNets.add(bottomNegative.getNet());
    }

    public ContactPoint getHole(int col, int letter) {
        if (col >= 0 && col < rows && letter >= 0 && letter < cols) {
            return grid[col][letter];
        }
        return null;
    }

    public Net getNet(int col, int letter) {
        ContactPoint cp = getHole(col, letter);
        return cp != null ? cp.getNet() : null;
    }

    public List<Net> getAllNets() { return allNets; }

    /** All contact points (main grid + power rails). */
    public List<ContactPoint> getAllContactPoints() {
        List<ContactPoint> all = new ArrayList<>();
        for (int c = 0; c < rows; c++)
            for (int l = 0; l < cols; l++)
                if (grid[c][l] != null) all.add(grid[c][l]);
        all.addAll(topPositive.getPoints());
        all.addAll(topNegative.getPoints());
        all.addAll(bottomPositive.getPoints());
        all.addAll(bottomNegative.getPoints());
        return all;
    }
    public int getRows() { return rows; }

    public PowerRail getTopPositive() { return topPositive; }
    public PowerRail getTopNegative() { return topNegative; }
    public PowerRail getBottomPositive() { return bottomPositive; }
    public PowerRail getBottomNegative() { return bottomNegative; }

    public void propagateAllNets() {
        for (Net net : allNets) {
            net.propagate();
        }
    }

    /** Horizontal X for a numbered column 0..rows-1. */
    public double getHoleX(int col) {
        return boardX + col * HOLE_SPACING;
    }

    /** Vertical Y for a lettered hole-row 0..9. The grid sits below the two top power rails. */
    public double getHoleY(int letter) {
        double gridStartY = boardY + 2 * HOLE_SPACING + RAIL_GAP; // two power rail rows + gap
        double y = gridStartY + letter * HOLE_SPACING;
        if (letter >= 5) y += GAP_HEIGHT;
        return y;
    }

    public double getBoardWidth() {
        return (rows - 1) * HOLE_SPACING + BOARD_PADDING;
    }

    public double getBoardHeight() {
        // 2 top rails + RAIL_GAP + 10 letter rows + GAP_HEIGHT + RAIL_GAP + 2 bottom rails + 20px bottom margin
        return 2 * HOLE_SPACING + RAIL_GAP + 9 * HOLE_SPACING + GAP_HEIGHT + RAIL_GAP + 2 * HOLE_SPACING + HOLE_SPACING;
    }

    @Override
    public void render(GraphicsContext gc) {
        double totalWidth = getBoardWidth();
        double totalHeight = getBoardHeight();

        // Board background — dark to match IntelliJ theme
        gc.setFill(Color.rgb(40, 42, 46));
        gc.fillRoundRect(boardX - 20, boardY - 20, totalWidth, totalHeight, 10, 10);
        gc.setStroke(Color.rgb(60, 62, 66));
        gc.setLineWidth(2);
        gc.strokeRoundRect(boardX - 20, boardY - 20, totalWidth, totalHeight, 10, 10);

        // Top power rails (two horizontal rows: + then -)
        double topPosY = boardY;
        double topNegY = boardY + HOLE_SPACING;
        renderPowerRail(gc, topPosY, true, topPositive);
        renderPowerRail(gc, topNegY, false, topNegative);

        // Main grid letter labels (a-j) on the left
        gc.setFill(Color.rgb(130, 138, 145));
        gc.setFont(Font.font("Monospaced", 9));
        gc.setTextAlign(TextAlignment.RIGHT);
        String[] labels = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"};
        for (int letter = 0; letter < cols; letter++) {
            gc.fillText(labels[letter], boardX - 8, getHoleY(letter) + 3);
        }

        // Numbered column labels (1..N) above main grid, every 5
        gc.setTextAlign(TextAlignment.CENTER);
        double labelY = getHoleY(0) - 8;
        for (int col = 0; col < rows; col++) {
            if ((col + 1) % 5 == 0 || col == 0) {
                gc.fillText(String.valueOf(col + 1), getHoleX(col), labelY);
            }
        }

        // Center gap line through the middle
        double gapMidY = (getHoleY(4) + getHoleY(5)) / 2;
        gc.setStroke(Color.rgb(60, 62, 66));
        gc.setLineWidth(1);
        gc.strokeLine(getHoleX(0) - 5, gapMidY, getHoleX(rows - 1) + 5, gapMidY);

        // Draw main grid holes
        for (int col = 0; col < rows; col++) {
            for (int letter = 0; letter < cols; letter++) {
                double hx = getHoleX(col);
                double hy = getHoleY(letter);
                grid[col][letter].setCanvasPosition(hx, hy);

                gc.setFill(grid[col][letter].isOccupied() ? Color.rgb(70, 72, 78) : Color.rgb(18, 18, 20));
                gc.fillOval(hx - HOLE_RADIUS, hy - HOLE_RADIUS, HOLE_RADIUS * 2, HOLE_RADIUS * 2);
            }
        }

        // Bottom power rails
        double bottomPosY = getHoleY(9) + HOLE_SPACING + RAIL_GAP - HOLE_SPACING; // first bottom rail row
        double bottomNegY = bottomPosY + HOLE_SPACING;
        renderPowerRail(gc, bottomPosY, true, bottomPositive);
        renderPowerRail(gc, bottomNegY, false, bottomNegative);
    }

    /** Renders a single horizontal power rail row at the given Y. */
    private void renderPowerRail(GraphicsContext gc, double y, boolean positive, PowerRail rail) {
        double xStart = getHoleX(0);
        double xEnd = getHoleX(rows - 1);

        // Rail line
        gc.setStroke(positive ? Color.rgb(200, 70, 70) : Color.rgb(80, 130, 220));
        gc.setLineWidth(1.5);
        gc.strokeLine(xStart, y, xEnd, y);

        // Rail symbol on the left
        gc.setFill(positive ? Color.rgb(200, 70, 70) : Color.rgb(80, 130, 220));
        gc.setFont(Font.font("Monospaced", 10));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(positive ? "+" : "-", xStart - 8, y + 3);

        // Rail holes — also assign canvas positions so they're wireable
        for (int i = 0; i < rows; i++) {
            double hx = getHoleX(i);
            ContactPoint cp = rail.getPoint(i);
            if (cp != null) cp.setCanvasPosition(hx, y);
            gc.setFill(Color.rgb(18, 18, 20));
            gc.fillOval(hx - HOLE_RADIUS, y - HOLE_RADIUS, HOLE_RADIUS * 2, HOLE_RADIUS * 2);
        }
    }
}
