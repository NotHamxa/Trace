package com.trace.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Top-level record for a .trs (Trace Sub-circuit) file. The port list
 * isn't stored explicitly — it's derived from the InputPort/OutputPort
 * components inside the embedded circuit.
 */
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
