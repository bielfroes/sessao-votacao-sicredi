package com.froes.assembly.dto;


import jakarta.validation.constraints.NotBlank;

public record ElectorRequestDTO(@NotBlank String cpf) {
}

