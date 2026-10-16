package com.froes.assembly.entity;

import com.froes.assembly.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "voting_result_outbox")
public class VotingResultOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    @Column(name = "routing_key", nullable = false, length = 32)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    public VotingResultOutbox markPublished(OffsetDateTime publishedAt) {
        return new VotingResultOutbox(id, eventKey, surveyId, routingKey, payload,
                OutboxStatus.PUBLISHED, createdAt, publishedAt);
    }
}
