package com.logiclab.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Saved test rows. Each cell is a LogicState name ("HIGH"/"LOW"/"FLOATING"),
 * or null to mean "don't care" in the expected column.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TestCasesDTO(
        List<List<String>> inputs,
        List<List<String>> expected
) {}
