package com.froes.assembly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.With;

@Builder
@With
public record SurveyRequestDTO(@Positive Long minutes, @NotBlank @Size(max = 500) String question) {
}
