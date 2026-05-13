package com.trace.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubCircuitDTO(
        int version,
        String id,
        String name,
        String author,
        CircuitDTO circuit
) {
    public static final int CURRENT_VERSION = 1;
}
