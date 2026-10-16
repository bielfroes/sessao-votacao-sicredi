package com.froes.assembly.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.froes.assembly.dto.ResultDTO;
import com.froes.assembly.entity.VotingResultOutbox;
import com.froes.assembly.enums.OutboxStatus;
import com.froes.assembly.event.producer.MessengerPublisherService;
import com.froes.assembly.repository.VotingResultOutboxRepository;
import com.froes.assembly.service.outbox.VotingResultOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotingResultOutboxServiceTest {

    @Mock
    private VotingResultOutboxRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MessengerPublisherService publisherService;

    @InjectMocks
    private VotingResultOutboxService service;

    @Test
    void doesNotCreateDuplicateEventForTheSameSession() {
        ResultDTO result = mock(ResultDTO.class);
        when(repository.existsByEventKey("voting-session-result:10")).thenReturn(true);

        service.enqueue(10L, 20L, result);

        verify(repository, never()).save(any());
        verifyNoInteractions(objectMapper);
    }

    @Test
    void createsPendingEventOnlyOnceWhenSessionIsClosed() throws Exception {
        ResultDTO result = mock(ResultDTO.class);
        when(repository.existsByEventKey("voting-session-result:10")).thenReturn(false);
        when(objectMapper.writeValueAsString(result)).thenReturn("{}");

        service.enqueue(10L, 20L, result);

        verify(repository).save(argThat(event ->
                event.getEventKey().equals("voting-session-result:10")
                        && event.getSurveyId().equals(20L)
                        && event.getStatus() == OutboxStatus.PENDING
                        && event.getPayload().equals("{}")));
    }

    @Test
    void publishesPendingEventAndMarksItPublished() {
        VotingResultOutbox event = VotingResultOutbox.builder()
                .id(1L)
                .eventKey("voting-session-result:10")
                .surveyId(20L)
                .routingKey("Aprovado")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        when(repository.findPending(OutboxStatus.PENDING)).thenReturn(List.of(event));

        service.publishPending();

        verify(publisherService).publishSerializedResult(
                "voting-session-result:10", "Aprovado", "{}");
        verify(repository).save(argThat(saved -> saved.getStatus() == OutboxStatus.PUBLISHED));
    }
}
