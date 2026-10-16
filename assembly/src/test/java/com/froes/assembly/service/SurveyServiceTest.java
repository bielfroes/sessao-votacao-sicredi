package com.froes.assembly.service;

import com.froes.assembly.dto.AgendaRequestDTO;
import com.froes.assembly.entity.Survey;
import com.froes.assembly.enums.SurveyStatusEnum;
import com.froes.assembly.helper.MessageHelper;
import com.froes.assembly.repository.SurveyRepository;
import com.froes.assembly.repository.VotingSessionRepository;
import com.froes.assembly.service.survey.SurveyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static com.froes.assembly.util.creator.SurveyCreator.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(SpringExtension.class)
public class SurveyServiceTest {
    @InjectMocks
    private SurveyService service;
    @Mock
    private SurveyRepository repository;
    @Mock
    private VotingSessionRepository sessionRepository;
    @Mock
    private MessageHelper messageHelper;

    @Test
    void create_returnsSurveyDTO_WhenSuccessful() {
        when(repository.save(any())).thenReturn(survey);
        assertEquals(surveyDTO, service.create(createSurveyRequestDTO()));
    }

    @Test
    void createAgenda_startsAsNotStartedAndDoesNotCreateVotingSession() {
        when(repository.save(any())).thenAnswer(invocation ->
                ((Survey) invocation.getArgument(0)).withId(23L));

        var created = service.createAgenda(new AgendaRequestDTO("Pauta aguardando votação"));

        assertEquals(SurveyStatusEnum.NOT_STARTED, created.status());
        assertNull(created.endTime());
        verify(repository).save(argThat(saved -> saved.getStatus() == SurveyStatusEnum.NOT_STARTED));
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void createAgenda_rejectsDuplicateQuestion() {
        when(repository.existsByQuestion("Pauta já cadastrada")).thenReturn(true);

        var exception = assertThrows(ResponseStatusException.class,
                () -> service.createAgenda(new AgendaRequestDTO("Pauta já cadastrada")));

        assertEquals(CONFLICT, exception.getStatusCode());
        assertEquals("Já existe uma pauta cadastrada com essa descrição.", exception.getReason());
        verify(repository, never()).save(any());
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void update_returnsSurveyDTO_WhenSuccessful() {
        Survey notStartedSurvey = survey.withStatus(SurveyStatusEnum.NOT_STARTED).withDurationMinutes(1L);
        when(repository.findById(any())).thenReturn(Optional.of(notStartedSurvey));
        when(repository.existsByQuestionAndIdNot(survey.getQuestion(), survey.getId())).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.update(survey.getId(), surveyUpdateDTO());

        assertEquals(survey.getQuestion(), updated.question());
        assertEquals(2L, updated.minutes());
        verify(repository).save(argThat(saved -> saved.getDurationMinutes() == 2L
                && saved.getQuestion().equals(survey.getQuestion())
                && saved.getStatus() == SurveyStatusEnum.NOT_STARTED));
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void update_rejectsSurveyThatIsNotNotStarted() {
        when(repository.findById(survey.getId())).thenReturn(Optional.of(survey));

        var exception = assertThrows(ResponseStatusException.class,
                () -> service.update(survey.getId(), surveyUpdateDTO()));

        assertEquals(CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(any());
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void update_rejectsDuplicateQuestion() {
        Survey notStartedSurvey = survey.withStatus(SurveyStatusEnum.NOT_STARTED);
        when(repository.findById(survey.getId())).thenReturn(Optional.of(notStartedSurvey));
        when(repository.existsByQuestionAndIdNot(survey.getQuestion(), survey.getId())).thenReturn(true);

        var exception = assertThrows(ResponseStatusException.class,
                () -> service.update(survey.getId(), surveyUpdateDTO()));

        assertEquals(CONFLICT, exception.getStatusCode());
        verify(repository, never()).save(any());
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void findById_throws404_whenSurveyNotFound() {
        when(repository.findById(survey.getId())).thenReturn(Optional.empty());
        final var exception = assertThrows(ResponseStatusException.class, () ->
                service.findById(survey.getId())).getStatusCode();
        assertEquals(NOT_FOUND, exception);
    }

    @Test
    void findDtoById_whenSuccessful() {
        when(repository.findById(survey.getId())).thenReturn(Optional.of(survey));
        assertEquals(surveyDTO, service.findDTOById(survey.getId()));
    }

    @Test
    void delete_removesEntity_whenSuccessful() {
        when(repository.findById(survey.getId())).thenReturn(Optional.of(survey));
        service.delete(survey.getId());
        verify(repository, times(1)).delete(survey);
    }

    @Test
    void findAll_returnsPageOfDTOs_whenSuccessful() {
        final var pageable = PageRequest.of(0, 10, Sort.by(ASC, "id"));
        final var assertion = new PageImpl<>(List.of(surveyDTO));
        final var surveyes = new PageImpl<>(List.of(survey));
        when(repository.findAll(any(Pageable.class))).thenReturn(surveyes);
        assertEquals(assertion, service.findAll(pageable));
    }

}
