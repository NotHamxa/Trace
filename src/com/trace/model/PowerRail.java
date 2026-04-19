package com.trace.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PowerRail implements Serializable {
    public enum RailType { POSITIVE, NEGATIVE }

    private RailType type;
    private List<ContactPoint> points;
    private Net net;

    public PowerRail(RailType type, int numPoints) {
        this.type = type;
        this.net = new Net();
        this.points = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            ContactPoint cp = new ContactPoint(0, i, net);
            points.add(cp);
        }
    }

    public RailType getType() {
        return type;
    }

    public Net getNet() {
        return net;
    }

    public List<ContactPoint> getPoints() {
        return points;
    }

    public ContactPoint getPoint(int index) {
        if (index >= 0 && index < points.size()) {
            return points.get(index);
        }
        return null;
    }
}
