package com.froes.assembly.entity;

import com.froes.assembly.enums.SurveyStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Getter
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "voting_session")
public class VotingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "survey_id", nullable = false)
    private Long surveyId;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Enumerated(STRING)
    @Column(nullable = false, length = 32)
    private SurveyStatusEnum status;

    @Version
    private long version;
}
