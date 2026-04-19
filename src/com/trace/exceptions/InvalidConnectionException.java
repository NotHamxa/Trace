package com.trace.exceptions;

public class InvalidConnectionException extends RuntimeException {
    public InvalidConnectionException(String message) {
        super(message);
    }
}
