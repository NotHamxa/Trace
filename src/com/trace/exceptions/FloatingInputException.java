package com.trace.exceptions;

public class FloatingInputException extends RuntimeException {
    private String pinLabel;
    private String componentId;

    public FloatingInputException(String pinLabel, String componentId) {
        super("Floating input: pin " + pinLabel + " on " + componentId);
        this.pinLabel = pinLabel;
        this.componentId = componentId;
    }

    public String getPinLabel() {
        return pinLabel;
    }

    public String getComponentId() {
        return componentId;
    }
}
