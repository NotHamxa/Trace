package com.trace.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PinRefDTO(
        String kind,
        String componentId,
        String pinLabel,
        Integer breadboardIndex,
        Integer col,
        Integer row,
        String rail
) {
    public static PinRefDTO component(String componentId, String pinLabel) {
        return new PinRefDTO("component", componentId, pinLabel, null, null, null, null);
    }

    public static PinRefDTO hole(int breadboardIndex, int col, int row) {
        return new PinRefDTO("hole", null, null, breadboardIndex, col, row, null);
    }

    public static PinRefDTO rail(int breadboardIndex, String rail, int col) {
        return new PinRefDTO("rail", null, null, breadboardIndex, col, null, rail);
    }
}
