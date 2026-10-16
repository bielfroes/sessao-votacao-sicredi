package com.froes.assembly.dto;

import com.froes.assembly.enums.SurveyStatusEnum;

import java.time.OffsetDateTime;

public record VotingSessionDTO(
        Long id,
        Long surveyId,
        OffsetDateTime openedAt,
        OffsetDateTime endTime,
        SurveyStatusEnum status) {
}
