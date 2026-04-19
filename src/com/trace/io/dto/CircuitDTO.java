package com.trace.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CircuitDTO(
        int version,
        String name,
        List<BreadboardDTO> breadboards,
        List<ComponentDTO> components,
        List<WireDTO> wires,
        TestCasesDTO tests
) {
    public static final int CURRENT_VERSION = 1;
}
