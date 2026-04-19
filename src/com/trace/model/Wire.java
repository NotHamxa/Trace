package com.trace.model;

import com.trace.interfaces.Renderable;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Wire implements Serializable, Renderable {
    private static final long serialVersionUID = 1L;
    private Pin startPin;
    private Pin endPin;
    private transient Color color;
    private double colorR, colorG, colorB;
    private List<double[]> waypoints; // Using double[] instead of Point2D for Serializable
    private LogicState currentState;
    private boolean locked;

    public Wire(Pin start, Pin end) {
        this.startPin = start;
        this.endPin = end;
        this.waypoints = new ArrayList<>();
        setColor(Color.BLACK);
        this.currentState = LogicState.FLOATING;
    }

    public Pin getStartPin() {
        return startPin;
    }

    public Pin getEndPin() {
        return endPin;
    }

    /** Reassigns the start pin — used when rerouting by dragging a wire endpoint. */
    public void setStartPin(Pin startPin) {
        this.startPin = startPin;
    }

    /** Reassigns the end pin — used when rerouting by dragging a wire endpoint. */
    public void setEndPin(Pin endPin) {
        this.endPin = endPin;
    }

    public Color getColor() {
        if (color == null) color = new Color(colorR, colorG, colorB, 1.0);
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        this.colorR = color.getRed();
        this.colorG = color.getGreen();
        this.colorB = color.getBlue();
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public LogicState getCurrentState() {
        return currentState;
    }

    public void resetState() {
        this.currentState = LogicState.FLOATING;
    }

    public void propagate() {
        LogicState startState = startPin.getState();
        LogicState endState = endPin.getState();

        // Only propagate non-floating states; prefer the side that has a definite value
        if (startState != LogicState.FLOATING && endState == LogicState.FLOATING) {
            endPin.setState(startState);
            currentState = startState;
        } else if (endState != LogicState.FLOATING && startState == LogicState.FLOATING) {
            startPin.setState(endState);
            currentState = endState;
        } else if (startState != LogicState.FLOATING) {
            // Both non-floating: prefer output/power/ground pin as driver
            boolean startDrives = startPin.getType() == PinType.OUTPUT
                    || startPin.getType() == PinType.POWER
                    || startPin.getType() == PinType.GROUND;
            if (startDrives) {
                endPin.setState(startState);
                currentState = startState;
            } else {
                startPin.setState(endState);
                currentState = endState;
            }
        } else {
            currentState = LogicState.FLOATING;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        render(gc, false);
    }

    public void render(GraphicsContext gc, boolean simulateMode) {
        double startX = startPin.getX();
        double startY = startPin.getY();
        double endX = endPin.getX();
        double endY = endPin.getY();

        gc.setLineWidth(2);

        if (simulateMode) {
            switch (currentState) {
                case HIGH:
                    gc.setStroke(Color.LIMEGREEN);
                    gc.setLineDashes();
                    break;
                case LOW:
                    gc.setStroke(Color.DARKBLUE);
                    gc.setLineDashes();
                    break;
                case FLOATING:
                    gc.setStroke(Color.GRAY);
                    gc.setLineDashes(5, 5);
                    break;
            }
        } else {
            gc.setStroke(getColor());
            gc.setLineDashes();
        }

        // Draw line through waypoints
        gc.beginPath();
        gc.moveTo(startX, startY);
        for (double[] wp : waypoints) {
            gc.lineTo(wp[0], wp[1]);
        }
        gc.lineTo(endX, endY);
        gc.stroke();

        // Reset dashes
        gc.setLineDashes();

        // Draw connection dots at endpoints.
        Color dotColor = gc.getStroke() instanceof Color ? (Color) gc.getStroke() : Color.BLACK;
        gc.setFill(dotColor);
        gc.fillOval(startX - 3, startY - 3, 6, 6);
        gc.fillOval(endX - 3, endY - 3, 6, 6);
    }

    public List<double[]> getWaypoints() {
        return waypoints;
    }

    /**
     * Returns the index of the waypoint (pivot) nearest to (px, py) within
     * {@code threshold} pixels, or -1 if no waypoint is within range.
     */
    public int findWaypointIndex(double px, double py, double threshold) {
        int bestIdx = -1;
        double bestDistSq = threshold * threshold;
        for (int i = 0; i < waypoints.size(); i++) {
            double[] wp = waypoints.get(i);
            double dx = wp[0] - px;
            double dy = wp[1] - py;
            double d = dx * dx + dy * dy;
            if (d <= bestDistSq) {
                bestDistSq = d;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    /** Removes the waypoint at {@code index}. */
    public void removeWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.remove(index);
        }
    }

    /** Insert a pivot point on the segment closest to (px,py). */
    public void addWaypointAt(double px, double py) {
        List<double[]> allPoints = new ArrayList<>();
        allPoints.add(new double[]{startPin.getX(), startPin.getY()});
        allPoints.addAll(waypoints);
        allPoints.add(new double[]{endPin.getX(), endPin.getY()});

        int bestSeg = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < allPoints.size() - 1; i++) {
            double[] a = allPoints.get(i);
            double[] b = allPoints.get(i + 1);
            double d = distanceToSegment(px, py, a[0], a[1], b[0], b[1]);
            if (d < bestDist) {
                bestDist = d;
                bestSeg = i;
            }
        }
        waypoints.add(bestSeg, new double[]{px, py});
    }

    public boolean isNear(double px, double py, double threshold) {
        // Check distance from point to each line segment
        double sx = startPin.getX(), sy = startPin.getY();

        List<double[]> allPoints = new ArrayList<>();
        allPoints.add(new double[]{sx, sy});
        allPoints.addAll(waypoints);
        allPoints.add(new double[]{endPin.getX(), endPin.getY()});

        for (int i = 0; i < allPoints.size() - 1; i++) {
            double[] a = allPoints.get(i);
            double[] b = allPoints.get(i + 1);
            if (distanceToSegment(px, py, a[0], a[1], b[0], b[1]) < threshold) {
                return true;
            }
        }
        return false;
    }

    private double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSq = dx * dx + dy * dy;
        if (lengthSq == 0) return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));

        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lengthSq));
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;
        return Math.sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY));
    }
}
