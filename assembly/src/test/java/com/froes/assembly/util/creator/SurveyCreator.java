package com.froes.assembly.util.creator;


import com.froes.assembly.dto.SurveyDTO;
import com.froes.assembly.dto.SurveyRequestDTO;
import com.froes.assembly.dto.SurveyUpdateDTO;
import com.froes.assembly.entity.Survey;
import com.froes.assembly.enums.SurveyStatusEnum;

import java.time.LocalDateTime;

import static com.froes.assembly.util.PodamFactory.podam;
import static com.froes.assembly.util.mapper.MapperConstants.surveyMapper;


public class SurveyCreator {

    public static final Survey survey = podam.manufacturePojo(Survey.class)
            .withEndTime(LocalDateTime.now().plusMinutes(1L))
            .withDurationMinutes(1L)
            .withStatus(SurveyStatusEnum.CLOSED);
    public static final SurveyDTO surveyDTO = surveyMapper.buildSurveyDTO(survey);

    public static SurveyRequestDTO createSurveyRequestDTO() {
        return SurveyRequestDTO.builder()
                .minutes(2L)
                .question(survey.getQuestion())
                .build();
    }

    public static SurveyUpdateDTO surveyUpdateDTO() {
        return SurveyUpdateDTO.builder()
                .minutes(2L)
                .question(survey.getQuestion())
                .build();
    }
}
