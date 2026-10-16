package com.froes.assembly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public record SurveyUpdateDTO(
        @Positive Long minutes,
        @NotBlank @Size(max = 500) String question) {
    @Builder public SurveyUpdateDTO {};
}
