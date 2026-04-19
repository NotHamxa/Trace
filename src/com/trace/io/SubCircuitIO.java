package com.trace.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.trace.io.dto.CircuitDTO;
import com.trace.io.dto.SubCircuitDTO;
import com.trace.model.subcircuit.SubCircuitDefinition;

import java.io.File;
import java.io.IOException;

/** Reads/writes .trs sub-circuit definition files. */
public final class SubCircuitIO {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SubCircuitIO() {}

    public static void write(SubCircuitDefinition def, String author, File file) throws IOException {
        CircuitDTO inner = CircuitJsonWriter.toDto(def.getInner());
        SubCircuitDTO dto = new SubCircuitDTO(
                SubCircuitDTO.CURRENT_VERSION,
                def.getId(),
                def.getName(),
                author,
                inner
        );
        MAPPER.writeValue(file, dto);
    }

    public static SubCircuitDefinition read(File file) throws IOException {
        SubCircuitDTO dto = MAPPER.readValue(file, SubCircuitDTO.class);
        if (dto.circuit() == null) throw new IOException("Malformed .trs file: missing circuit");
        return new SubCircuitDefinition(
                dto.id(),
                dto.name() == null ? dto.id() : dto.name(),
                CircuitJsonReader.fromDto(dto.circuit())
        );
    }
}
