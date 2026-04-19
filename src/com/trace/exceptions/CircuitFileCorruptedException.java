package com.trace.exceptions;

import java.io.IOException;

public class CircuitFileCorruptedException extends IOException {
    public CircuitFileCorruptedException(String message) {
        super(message);
    }
}
