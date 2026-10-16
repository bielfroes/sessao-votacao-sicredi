package com.froes.assembly.service;

import com.froes.assembly.dto.VotingSessionRequestDTO;
import com.froes.assembly.entity.VotingSession;
import com.froes.assembly.enums.SurveyStatusEnum;
import com.froes.assembly.repository.VotingSessionRepository;
import com.froes.assembly.service.session.VotingSessionService;
import com.froes.assembly.service.survey.SurveyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.froes.assembly.util.creator.SurveyCreator.survey;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.CONFLICT;

@ExtendWith(SpringExtension.class)
class VotingSessionServiceTest {

    @InjectMocks
    private VotingSessionService service;

    @Mock
    private VotingSessionRepository repository;

    @Mock
    private SurveyService surveyService;

    @BeforeEach
    void configureDefaults() {
        ReflectionTestUtils.setField(service, "applicationTimeZone", "America/Sao_Paulo");
        ReflectionTestUtils.setField(service, "defaultDurationMinutes", 1L);
        when(surveyService.findById(survey.getId())).thenReturn(survey);
        when(surveyService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findFirstBySurveyIdOrderByOpenedAtDesc(any())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void opensSessionWithOneMinuteDefault() {
        var session = service.open(survey.getId(), new VotingSessionRequestDTO(null));

        assertEquals(SurveyStatusEnum.OPEN, session.status());
        assertEquals(60, session.endTime().toEpochSecond() - session.openedAt().toEpochSecond());
        verify(surveyService).save(argThat(updated ->
                updated.getStatus() == SurveyStatusEnum.OPEN && updated.getEndTime() != null));
    }

    @Test
    void usesConfiguredSurveyDurationWhenOpeningWithoutMinutes() {
        var configuredSurvey = survey.withDurationMinutes(7L);
        when(surveyService.findById(survey.getId())).thenReturn(configuredSurvey);

        var session = service.open(survey.getId(), new VotingSessionRequestDTO(null));

        assertEquals(7 * 60, session.endTime().toEpochSecond() - session.openedAt().toEpochSecond());
    }

    @Test
    void rejectsOpeningAnotherActiveSession() {
        VotingSession current = activeSession(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(2));
        when(repository.findFirstBySurveyIdOrderByOpenedAtDesc(survey.getId())).thenReturn(Optional.of(current));

        assertEquals(CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.open(survey.getId(), new VotingSessionRequestDTO(5L))).getStatusCode());
    }

    @Test
    void rejectsOpeningClosedSessionAndRequiresANewSurvey() {
        VotingSession closed = activeSession(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1))
                .withStatus(SurveyStatusEnum.CLOSED);
        when(repository.findFirstBySurveyIdOrderByOpenedAtDesc(survey.getId())).thenReturn(Optional.of(closed));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.open(survey.getId(), new VotingSessionRequestDTO(5L)));

        assertEquals(CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("já foi encerrada"));
        verify(repository, never()).saveAndFlush(any());
        verify(surveyService, never()).save(any());
    }

    @Test
    void returnsConflictWhenDatabaseRejectsConcurrentSessionOpen() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "active session constraint", new SQLException("duplicate", "23505")));

        assertEquals(CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.open(survey.getId(), new VotingSessionRequestDTO(1L))).getStatusCode());
    }

    @Test
    void expiredSessionCannotBeReopened() {
        VotingSession expired = activeSession(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(repository.findFirstBySurveyIdOrderByOpenedAtDesc(survey.getId())).thenReturn(Optional.of(expired));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.open(survey.getId(), new VotingSessionRequestDTO(2L)));

        assertEquals(CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("já foi encerrada"));
        verify(repository, never()).saveAndFlush(any());
        verify(surveyService, never()).save(any());
    }

    @Test
    void rejectsExpiredSessionWhenRegisteringVote() {
        when(repository.findFirstBySurveyIdAndStatusOrderByOpenedAtDesc(
                survey.getId(), SurveyStatusEnum.OPEN))
                .thenReturn(Optional.of(activeSession(OffsetDateTime.now(ZoneOffset.UTC).minusNanos(1))));

        assertEquals(CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.requireOpen(survey.getId())).getStatusCode());
    }

    @Test
    void rejectsVoteBeforeSessionStarts() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        VotingSession futureSession = VotingSession.builder()
                .id(1L)
                .surveyId(survey.getId())
                .openedAt(now.plusSeconds(1))
                .endTime(now.plusMinutes(1))
                .status(SurveyStatusEnum.OPEN)
                .build();
        when(repository.findFirstBySurveyIdAndStatusOrderByOpenedAtDesc(
                survey.getId(), SurveyStatusEnum.OPEN)).thenReturn(Optional.of(futureSession));

        assertEquals(CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.requireOpen(survey.getId())).getStatusCode());
    }

    private VotingSession activeSession(OffsetDateTime endTime) {
        return VotingSession.builder()
                .id(1L)
                .surveyId(survey.getId())
                .openedAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1))
                .endTime(endTime)
                .status(SurveyStatusEnum.OPEN)
                .build();
    }
}
