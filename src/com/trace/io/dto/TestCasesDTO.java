package com.trace.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TestCasesDTO(
        List<List<String>> inputs,
        List<List<String>> expected
) {}
