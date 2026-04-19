package com.logiclab.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WireDTO(
        PinRefDTO start,
        PinRefDTO end,
        String colorHex,
        boolean locked,
        List<double[]> waypoints
) {}
