package com.froes.assembly.service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.froes.assembly.dto.ResultDTO;
import com.froes.assembly.entity.VotingResultOutbox;
import com.froes.assembly.enums.OutboxStatus;
import com.froes.assembly.repository.VotingResultOutboxRepository;
import com.froes.assembly.event.producer.MessengerPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.froes.assembly.util.database.DatabaseConstraintUtils.isUniqueViolation;

@Slf4j
@Service
@RequiredArgsConstructor
public class VotingResultOutboxService {

    private final VotingResultOutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final MessengerPublisherService publisherService;

    public void enqueue(Long sessionId, Long surveyId, ResultDTO result) {
        String eventKey = "voting-session-result:" + sessionId;
        if (repository.existsByEventKey(eventKey)) {
            return;
        }
        try {
            repository.save(VotingResultOutbox.builder()
                    .eventKey(eventKey)
                    .surveyId(surveyId)
                    .routingKey(result.result())
                    .payload(objectMapper.writeValueAsString(result))
                    .status(OutboxStatus.PENDING)
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível serializar o resultado da votação", exception);
        } catch (DataIntegrityViolationException exception) {
            if (!isUniqueViolation(exception)) {
                throw exception;
            }
            log.info("Evento de resultado {} já foi criado por outra instância", eventKey);
        }
    }

    @Transactional
    public void publishPending() {
        for (VotingResultOutbox event : repository.findPending(OutboxStatus.PENDING)) {
            publisherService.publishSerializedResult(event.getEventKey(), event.getRoutingKey(), event.getPayload());
            repository.save(event.markPublished(OffsetDateTime.now(ZoneOffset.UTC)));
        }
    }
}
