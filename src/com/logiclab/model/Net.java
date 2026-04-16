package com.logiclab.model;

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

    /** Returns all pins on this net: occupant component pins AND HolePins (wire endpoints). */
    public List<Pin> getConnectedPins() {
        List<Pin> connectedPins = new ArrayList<>();
        for (ContactPoint cp : points) {
            if (cp.isOccupied()) {
                connectedPins.add(cp.getOccupant());
            }
            // Also include HolePins (wire endpoints on bare holes)
            Pin hp = cp.getHolePinIfExists();
            if (hp != null) {
                connectedPins.add(hp);
            }
        }
        return connectedPins;
    }

    public void propagate() {
        // Priority-based scan of visible pins (occupants + existing HolePins).
        // HolePins are transient on ContactPoint, so after deserialization they may
        // be invisible here — but they still write net.state via HolePin.setState().
        // We must preserve that cached state as a fallback.

        LogicState drivenState = LogicState.FLOATING;

        // 1. POWER/GROUND pins — highest priority
        for (Pin p : getConnectedPins()) {
            if (p.getState() != LogicState.FLOATING) {
                PinType t = p.getType();
                if (t == PinType.POWER || t == PinType.GROUND) {
                    drivenState = p.getState();
                    break;
                }
            }
        }

        // 2. OUTPUT pins (IC outputs are authoritative)
        if (drivenState == LogicState.FLOATING) {
            for (Pin p : getConnectedPins()) {
                if (p.getState() != LogicState.FLOATING && p.getType() == PinType.OUTPUT) {
                    drivenState = p.getState();
                    break;
                }
            }
        }

        // 3. Any other non-floating visible pin
        if (drivenState == LogicState.FLOATING) {
            for (Pin p : getConnectedPins()) {
                if (p.getState() != LogicState.FLOATING) {
                    drivenState = p.getState();
                    break;
                }
            }
        }

        // 4. Fall back to cached net state (written by invisible HolePins via wire propagation)
        if (drivenState == LogicState.FLOATING) {
            drivenState = state;
        }

        state = drivenState;
        if (drivenState != LogicState.FLOATING) {
            for (Pin p : getConnectedPins()) {
                // Don't override OUTPUT pins — they are set by IC.simulate()
                if (p.getType() != PinType.OUTPUT) {
                    p.setState(drivenState);
                }
            }
        }
    }
}
