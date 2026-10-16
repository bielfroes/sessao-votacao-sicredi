package com.froes.assembly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MobileActionDTO(
        String texto,
        String url,
        Map<String, Object> body) {
}
