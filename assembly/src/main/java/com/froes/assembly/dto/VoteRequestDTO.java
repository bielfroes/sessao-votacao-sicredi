package com.froes.assembly.dto;

import jakarta.validation.constraints.NotNull;
import com.froes.assembly.enums.VoteChoice;
import lombok.Builder;

public record VoteRequestDTO(
        @NotNull Long surveyId,
        @NotNull Long electorId,
        @NotNull VoteChoice approval) {
    @Builder
    public VoteRequestDTO {};
}
