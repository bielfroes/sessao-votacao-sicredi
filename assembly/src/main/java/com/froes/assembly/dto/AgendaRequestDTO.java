package com.froes.assembly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgendaRequestDTO(@NotBlank @Size(max = 500) String question) {
}
