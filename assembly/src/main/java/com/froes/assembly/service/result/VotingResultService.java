package com.froes.assembly.service.result;

import com.froes.assembly.dto.ResultDTO;
import com.froes.assembly.dto.SurveyDTO;
import com.froes.assembly.entity.Survey;
import com.froes.assembly.enums.ResultStatusEnum;
import com.froes.assembly.enums.SurveyStatusEnum;
import com.froes.assembly.repository.SurveyVoteCounts;
import com.froes.assembly.repository.VoteCounts;
import com.froes.assembly.repository.VoteRepository;
import com.froes.assembly.service.survey.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.froes.assembly.util.mapper.MapperConstants.surveyMapper;

@Service
@RequiredArgsConstructor
public class VotingResultService {

    private final VoteRepository repository;
    private final SurveyService surveyService;

    @Value("${app.time-zone:America/Sao_Paulo}")
    private String applicationTimeZone = "America/Sao_Paulo";

    public ResultDTO findBySurveyId(Long surveyId) {
        SurveyDTO survey = surveyService.findDTOById(surveyId);
        VoteCounts counts = repository.countVotesBySurveyId(surveyId);
        return buildResult(survey, counts.approves(), counts.reproves());
    }

    public List<ResultDTO> findAll() {
        List<Survey> surveys = surveyService.findAll();
        if (surveys.isEmpty()) {
            return List.of();
        }
        List<Long> surveyIds = surveys.stream().map(Survey::getId).toList();
        Map<Long, SurveyVoteCounts> countsBySurvey = repository.countVotesBySurveyIds(surveyIds).stream()
                .collect(Collectors.toMap(SurveyVoteCounts::surveyId, counts -> counts));
        List<ResultDTO> results = new ArrayList<>(surveys.size());
        for (Survey survey : surveys) {
            SurveyVoteCounts counts = countsBySurvey.getOrDefault(survey.getId(),
                    new SurveyVoteCounts(survey.getId(), 0L, 0L));
            results.add(buildResult(surveyMapper.buildSurveyDTO(survey),
                    counts.approves(), counts.reproves()));
        }
        return results;
    }

    private ResultDTO buildResult(SurveyDTO survey, Long approves, Long reproves) {
        String result = approves > reproves ? "Aprovado" : approves < reproves ? "Reprovado" : "Empate";
        ResultStatusEnum status = switch (survey.status()) {
            case NOT_STARTED -> ResultStatusEnum.NOT_STARTED;
            case OPEN -> survey.endTime() != null
                    && !survey.endTime().isBefore(LocalDateTime.now(ZoneId.of(applicationTimeZone)))
                    ? ResultStatusEnum.IN_PROGRESS
                    : ResultStatusEnum.FINISHED;
            case CLOSED -> ResultStatusEnum.FINISHED;
        };
        return ResultDTO.builder()
                .survey(survey)
                .approves(approves)
                .reproves(reproves)
                .totalVotes(approves + reproves)
                .result(result)
                .status(status)
                .build();
    }
}
