package com.froes.assembly.dto;


import com.froes.assembly.enums.SurveyStatusEnum;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record SurveyDTO(
        Long id,
        LocalDateTime endTime,
        String question,
        SurveyStatusEnum status,
        OffsetDateTime createdAt,
        long minutes) {
}
