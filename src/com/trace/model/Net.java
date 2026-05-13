package com.trace.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Net implements Serializable {
    private List<ContactPoint> points;
    private LogicState state;

    public Net() {
        this.points = new ArrayList<>();
        this.state = LogicState.FLOATING;
    }

    public void addPoint(ContactPoint cp) {
        points.add(cp);
    }

    public LogicState getState() {
        return state;
    }

    public void setState(LogicState state) {
        this.state = state;
    }

    public List<ContactPoint> getPoints() {
        return points;
    }

    public List<Pin> getConnectedPins() {
        List<Pin> connectedPins = new ArrayList<>();
        for (ContactPoint cp : points) {
            if (cp.isOccupied()) {
                connectedPins.add(cp.getOccupant());
            }
            Pin hp = cp.getHolePinIfExists();
            if (hp != null) {
                connectedPins.add(hp);
            }
        }
        return connectedPins;
    }

    public void propagate() {

        LogicState drivenState = LogicState.FLOATING;

        for (Pin p : getConnectedPins()) {
            if (p.getState() != LogicState.FLOATING) {
                PinType t = p.getType();
                if (t == PinType.POWER || t == PinType.GROUND) {
                    drivenState = p.getState();
                    break;
                }
            }
        }

        if (drivenState == LogicState.FLOATING) {
            for (Pin p : getConnectedPins()) {
                if (p.getState() != LogicState.FLOATING && p.getType() == PinType.OUTPUT) {
                    drivenState = p.getState();
                    break;
                }
            }
        }

        if (drivenState == LogicState.FLOATING) {
            for (Pin p : getConnectedPins()) {
                if (p.getState() != LogicState.FLOATING) {
                    drivenState = p.getState();
                    break;
                }
            }
        }

        if (drivenState == LogicState.FLOATING) {
            drivenState = state;
        }

        state = drivenState;
        if (drivenState != LogicState.FLOATING) {
            for (Pin p : getConnectedPins()) {
                if (p.getType() != PinType.OUTPUT) {
                    p.setState(drivenState);
                }
            }
        }
    }
}
