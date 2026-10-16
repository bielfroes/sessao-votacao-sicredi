package com.froes.assembly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MobileScreenDTO(
        String tipo,
        String titulo,
        List<? extends MobileScreenItemDTO> itens,
        MobileActionDTO botaoOk,
        MobileActionDTO botaoCancelar) {
}
