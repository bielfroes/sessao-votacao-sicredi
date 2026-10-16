package com.froes.assembly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MobileFieldDTO(
        String tipo,
        String texto,
        String id,
        String titulo,
        Object valor,
        @JsonAnyGetter Map<String, Object> additionalProperties)
        implements MobileScreenItemDTO {

    public static MobileFieldDTO text(String texto) {
        return text(texto, Map.of());
    }

    public static MobileFieldDTO text(String texto, Map<String, Object> additionalProperties) {
        return new MobileFieldDTO("TEXTO", texto, null, null, null, additionalProperties);
    }

    public static MobileFieldDTO input(String tipo, String id, String titulo, Object valor) {
        return input(tipo, id, titulo, valor, Map.of());
    }

    public static MobileFieldDTO input(
            String tipo, String id, String titulo, Object valor, Map<String, Object> additionalProperties) {
        return new MobileFieldDTO(tipo, null, id, titulo, valor, additionalProperties);
    }
}
