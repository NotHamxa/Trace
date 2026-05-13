package com.trace.model;

import com.trace.interfaces.Interactable;

public abstract class InputComponent extends Component implements Interactable {
    protected LogicState outputState = LogicState.LOW;

    private PinSide pinSide = PinSide.TOP;

    public InputComponent(String name, double width, double height) {
        super(name, width, height);
    }

    @Override
    public void simulate() {
        getOutputPin().setState(outputState);
    }

    public PinSide getPinSide() {
        return pinSide == null ? PinSide.TOP : pinSide;
    }

    public void setPinSide(PinSide side) {
        this.pinSide = side == null ? PinSide.TOP : side;
        updatePinPositions();
    }

    public abstract void onInteract();
    public abstract Pin getOutputPin();
}
