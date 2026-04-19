package com.logiclab.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComponentDTO(
        String id,
        String type,
        double x,
        double y,
        String displayLabel,
        boolean locked,
        Map<String, Object> props
) {}
