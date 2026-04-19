package com.logiclab.io.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A reference to a pin in the saved file. One of three kinds:
 *   - "component": a pin on a placed component (componentId + pinLabel)
 *   - "hole":      a bare main-grid hole (breadboardIndex + col + row, row 0..9 = a..j)
 *   - "rail":      a power-rail hole (breadboardIndex + rail + col)
 */
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
