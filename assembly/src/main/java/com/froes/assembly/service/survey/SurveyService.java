package com.froes.assembly.service.survey;


import com.froes.assembly.dto.AgendaRequestDTO;
import com.froes.assembly.dto.SurveyDTO;
import com.froes.assembly.dto.SurveyRequestDTO;
import com.froes.assembly.dto.SurveyUpdateDTO;
import com.froes.assembly.entity.Survey;
import com.froes.assembly.entity.VotingSession;
import com.froes.assembly.enums.SurveyStatusEnum;
import com.froes.assembly.helper.MessageHelper;
import com.froes.assembly.repository.SurveyRepository;
import com.froes.assembly.repository.VotingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static com.froes.assembly.exception.ErrorCodeEnum.ERROR_SURVEY_NOT_FOUND;
import static com.froes.assembly.util.mapper.MapperConstants.surveyMapper;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository repository;
    private final VotingSessionRepository sessionRepository;
    private final MessageHelper messageHelper;

    @Value("${app.time-zone:America/Sao_Paulo}")
    private String applicationTimeZone = "America/Sao_Paulo";

    @Value("${app.voting.default-duration-minutes:1}")
    private long defaultDurationMinutes = 1;

    @Transactional
    public SurveyDTO create(final SurveyRequestDTO requestDTO) {
        ensureQuestionIsUnique(requestDTO.question());
        long minutes = isNull(requestDTO.minutes()) ? defaultDurationMinutes : requestDTO.minutes();
        OffsetDateTime openedAt = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime endTime = openedAt.plusMinutes(minutes);
        Survey survey = repository.save(surveyMapper.buildSurvey(requestDTO)
                .withEndTime(endTime.atZoneSameInstant(ZoneId.of(applicationTimeZone)).toLocalDateTime())
                .withDurationMinutes(minutes)
                .withCreatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .withStatus(SurveyStatusEnum.OPEN));
        sessionRepository.saveAndFlush(VotingSession.builder()
                .surveyId(survey.getId())
                .openedAt(openedAt)
                .endTime(endTime)
                .status(SurveyStatusEnum.OPEN)
                .build());
        log.info("Pauta {} criada e sessão de votação aberta até {}", survey.getId(), endTime);
        return surveyMapper.buildSurveyDTO(survey);
    }

    @Transactional
    public SurveyDTO createAgenda(final AgendaRequestDTO requestDTO) {
        ensureQuestionIsUnique(requestDTO.question());
        Survey survey = repository.save(surveyMapper.buildSurvey(
                        new SurveyRequestDTO(null, requestDTO.question()))
                .withCreatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                        .withDurationMinutes(defaultDurationMinutes)
                        .withStatus(SurveyStatusEnum.NOT_STARTED));
        log.info("Pauta {} criada", survey.getId());
        return surveyMapper.buildSurveyDTO(survey);
    }

    @Transactional
    public SurveyDTO update(final Long surveyId, final SurveyUpdateDTO updateDTO) {
        Survey survey = findById(surveyId);
        if (survey.getStatus() != SurveyStatusEnum.NOT_STARTED) {
            throw new ResponseStatusException(CONFLICT,
                    "Somente pautas com status NOT_STARTED podem ser alteradas.");
        }

        ensureQuestionIsUnique(updateDTO.question(), survey.getId());
        long minutes = isNull(updateDTO.minutes()) ? survey.getDurationMinutes() : updateDTO.minutes();
        Survey updated = repository.save(survey
                .withQuestion(updateDTO.question())
                .withDurationMinutes(minutes));
        return surveyMapper.buildSurveyDTO(updated);
    }

    private void ensureQuestionIsUnique(String question) {
        if (repository.existsByQuestion(question)) {
            throw new ResponseStatusException(CONFLICT, "Já existe uma pauta cadastrada com essa descrição.");
        }
    }

    private void ensureQuestionIsUnique(String question, Long surveyId) {
        if (repository.existsByQuestionAndIdNot(question, surveyId)) {
            throw new ResponseStatusException(CONFLICT, "Já existe uma pauta cadastrada com essa descrição.");
        }
    }

    public Survey findById(final Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error(messageHelper.get(ERROR_SURVEY_NOT_FOUND, id.toString()));
            return new ResponseStatusException(NOT_FOUND, messageHelper.get(ERROR_SURVEY_NOT_FOUND, id.toString()));
        });
    }

    public SurveyDTO findDTOById(final Long id) {
        Survey survey = findById(id);
        return surveyMapper.buildSurveyDTO(survey);
    }

    public void delete(final Long id) {
        Survey survey = findById(id);
        repository.delete(survey);
    }

    public Page<SurveyDTO> findAll(final Pageable pageable) {
        return repository.findAll(pageable).map(surveyMapper::buildSurveyDTO);
    }

    public List<Survey> findAll() {
        return repository.findAll();
    }

    public Survey save(Survey survey){
        return repository.save(survey);
    };
}
