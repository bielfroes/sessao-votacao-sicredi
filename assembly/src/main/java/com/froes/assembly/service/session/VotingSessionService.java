package com.froes.assembly.service.session;

import com.froes.assembly.dto.VotingSessionDTO;
import com.froes.assembly.dto.VotingSessionRequestDTO;
import com.froes.assembly.entity.Survey;
import com.froes.assembly.entity.VotingSession;
import com.froes.assembly.enums.SurveyStatusEnum;
import com.froes.assembly.repository.VotingSessionRepository;
import com.froes.assembly.service.survey.SurveyService;
import com.froes.assembly.service.outbox.VotingResultOutboxService;
import com.froes.assembly.dto.ResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static com.froes.assembly.util.database.DatabaseConstraintUtils.isUniqueViolation;

@Slf4j
@Service
@RequiredArgsConstructor
public class VotingSessionService {

    private final VotingSessionRepository repository;
    private final SurveyService surveyService;
    private final com.froes.assembly.service.result.VotingResultService votingResultService;
    private final VotingResultOutboxService votingResultOutboxService;

    @Value("${app.time-zone:America/Sao_Paulo}")
    private String applicationTimeZone = "America/Sao_Paulo";

    @Value("${app.voting.default-duration-minutes:1}")
    private long defaultDurationMinutes = 1;

    @Transactional
    public VotingSessionDTO open(Long surveyId, VotingSessionRequestDTO request) {
        Survey survey = surveyService.findById(surveyId);
        long durationMinutes = request == null || request.minutes() == null
                ? (survey.getDurationMinutes() > 0 ? survey.getDurationMinutes() : defaultDurationMinutes)
                : request.minutes();
        if (durationMinutes <= 0) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "A duração da sessão deve ser maior que zero");
        }

        OffsetDateTime openedAt = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime endTime = openedAt.plusMinutes(durationMinutes);
        Optional<VotingSession> latestSession = repository.findFirstBySurveyIdOrderByOpenedAtDesc(surveyId);
        if (latestSession.isPresent()) {
            VotingSession previous = latestSession.get();
            if (previous.getStatus() == SurveyStatusEnum.OPEN
                    && previous.getEndTime().isAfter(openedAt)) {
                throw sessionAlreadyOpen(surveyId);
            }
            throw sessionAlreadyClosed(surveyId);
        }

        VotingSession session = VotingSession.builder()
                .surveyId(surveyId)
                .openedAt(openedAt)
                .endTime(endTime)
                .status(SurveyStatusEnum.OPEN)
                .build();

        try {
            VotingSession saved = repository.saveAndFlush(session);
            surveyService.save(survey
                    .withEndTime(endTime.atZoneSameInstant(ZoneId.of(applicationTimeZone)).toLocalDateTime())
                    .withStatus(SurveyStatusEnum.OPEN));
            log.info("Sessão de votação {} aberta para a pauta {} até {}", saved.getId(), surveyId, endTime);
            return toDto(saved);
        } catch (DataIntegrityViolationException exception) {
            if (!isUniqueViolation(exception)) {
                throw exception;
            }
            throw sessionAlreadyOpen(surveyId);
        }
    }

    public VotingSessionDTO requireOpen(Long surveyId) {
        VotingSession session = repository
                .findFirstBySurveyIdAndStatusOrderByOpenedAtDesc(surveyId, SurveyStatusEnum.OPEN)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                        "No open voting session exists for survey " + surveyId));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (now.isBefore(session.getOpenedAt())) {
            throw new ResponseStatusException(CONFLICT,
                    "A sessão de votação ainda não foi iniciada");
        }
        if (session.getEndTime().isBefore(now)) {
            throw new ResponseStatusException(CONFLICT, "A sessão de votação expirou");
        }
        return toDto(session);
    }

    @Transactional
    public List<Long> closeExpiredSessions() {
        List<VotingSession> expired = repository.lockExpiredSessions(OffsetDateTime.now(ZoneOffset.UTC));
        for (VotingSession session : expired) {
            repository.save(session.withStatus(SurveyStatusEnum.CLOSED));
            Survey survey = surveyService.findById(session.getSurveyId());
            surveyService.save(survey.withStatus(SurveyStatusEnum.CLOSED));
            ResultDTO result = votingResultService.findBySurveyId(session.getSurveyId());
            votingResultOutboxService.enqueue(session.getId(), session.getSurveyId(), result);
        }
        return expired.stream().map(VotingSession::getSurveyId).toList();
    }

    public Optional<VotingSessionDTO> latest(Long surveyId) {
        surveyService.findById(surveyId);
        return repository.findFirstBySurveyIdOrderByOpenedAtDesc(surveyId).map(this::toDto);
    }

    private VotingSessionDTO toDto(VotingSession session) {
        return new VotingSessionDTO(session.getId(), session.getSurveyId(), session.getOpenedAt(),
                session.getEndTime(), session.getStatus());
    }

    private ResponseStatusException sessionAlreadyOpen(Long surveyId) {
        log.warn("Já existe uma sessão de votação aberta para a pauta {}", surveyId);
        return new ResponseStatusException(CONFLICT,
                "Já existe uma sessão de votação aberta para a pauta " + surveyId);
    }

    private ResponseStatusException sessionAlreadyClosed(Long surveyId) {
        log.warn("A votação da pauta {} já foi encerrada ou expirou", surveyId);
        return new ResponseStatusException(CONFLICT,
                "A votação desta pauta já foi encerrada. Crie uma nova pauta para abrir outra votação.");
    }

}
