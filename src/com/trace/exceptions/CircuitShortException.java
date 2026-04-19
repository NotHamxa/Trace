package com.trace.exceptions;

public class CircuitShortException extends RuntimeException {
    public CircuitShortException(String message) {
        super(message);
    }
}
