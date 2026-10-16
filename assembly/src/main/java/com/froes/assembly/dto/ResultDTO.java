package com.froes.assembly.dto;

import com.froes.assembly.enums.ResultStatusEnum;
import lombok.Builder;

public record ResultDTO(SurveyDTO survey, Long approves, Long reproves, Long totalVotes, String result, ResultStatusEnum status) {
    @Builder
    public ResultDTO {};
}
