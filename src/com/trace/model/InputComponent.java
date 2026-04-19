package com.trace.model;

import com.trace.interfaces.Interactable;

public abstract class InputComponent extends Component implements Interactable {
    protected LogicState outputState = LogicState.LOW;

    /** Side of the body where the output pin(s) exit. Defaults to TOP. */
    private PinSide pinSide = PinSide.TOP;

    public InputComponent(String name, double width, double height) {
        super(name, width, height);
    }

    @Override
    public void simulate() {
        getOutputPin().setState(outputState);
    }

    /** Null-safe so older serialized circuits (field missing) default to TOP. */
    public PinSide getPinSide() {
        return pinSide == null ? PinSide.TOP : pinSide;
    }

    /**
     * Changes which side the output pin(s) exit from. Callers that care
     * about breadboard-hole attachment must rip pins out of their current
     * holes before calling this and reinsert afterward.
     */
    public void setPinSide(PinSide side) {
        this.pinSide = side == null ? PinSide.TOP : side;
        updatePinPositions();
    }

    public abstract void onInteract();
    public abstract Pin getOutputPin();
}
