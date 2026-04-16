package com.logiclab.model;

/**
 * A Pin that lives on a breadboard ContactPoint instead of a real component.
 * Reads/writes its state through the underlying Net so wires connected to bare
 * holes participate in normal simulation.
 */
public class HolePin extends Pin {
    private final ContactPoint contact;

    public HolePin(ContactPoint contact, Component owner) {
        super("hole", PinType.INPUT, owner);
        this.contact = contact;
        setPosition(contact.getCanvasX(), contact.getCanvasY());
    }

    public ContactPoint getContact() {
        return contact;
    }

    @Override
    public LogicState getState() {
        LogicState local = super.getState();
        if (local != LogicState.FLOATING) return local;
        return contact.getNet().getState();
    }

    @Override
    public void setState(LogicState state) {
        super.setState(state);
        if (state != LogicState.FLOATING) {
            contact.getNet().setState(state);
        }
    }

    @Override
    public double getX() {
        return contact.getCanvasX();
    }

    @Override
    public double getY() {
        return contact.getCanvasY();
    }
}
