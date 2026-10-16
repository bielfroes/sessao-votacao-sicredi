package com.froes.assembly.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import com.froes.assembly.enums.VoteChoice;

public record VoteUpdateDTO(
        @NotNull Long id,
        @NotNull Long surveyId,
        @NotNull Long electorId,
        @NotNull VoteChoice approval) {
    @Builder public VoteUpdateDTO {};
}
