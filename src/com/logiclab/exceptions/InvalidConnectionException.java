package com.logiclab.exceptions;

public class InvalidConnectionException extends RuntimeException {
    public InvalidConnectionException(String message) {
        super(message);
    }
}
