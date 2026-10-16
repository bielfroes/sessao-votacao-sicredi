package com.froes.assembly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MobileSelectionItemDTO(
        String texto,
        String url,
        Map<String, Object> body,
        @JsonAnyGetter Map<String, Object> additionalProperties) implements MobileScreenItemDTO {

    public MobileSelectionItemDTO(String texto, String url, Map<String, Object> body) {
        this(texto, url, body, Map.of());
    }
}
