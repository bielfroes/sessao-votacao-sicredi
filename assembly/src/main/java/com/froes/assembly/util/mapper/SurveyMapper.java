package com.froes.assembly.util.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.froes.assembly.dto.SurveyDTO;
import com.froes.assembly.dto.SurveyRequestDTO;
import com.froes.assembly.entity.Survey;

@Mapper
public interface SurveyMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    Survey buildSurvey(SurveyRequestDTO requestDTO);

    @Mapping(target = "minutes", source = "durationMinutes")
    SurveyDTO buildSurveyDTO(Survey survey);
}
