package com.froes.assembly.entity;

import com.froes.assembly.enums.SurveyStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "survey")
public class Survey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime endTime;
    @Builder.Default
    @Column(name = "duration_minutes", nullable = false)
    private long durationMinutes = 1L;
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(length = 500)
    private String question;
    @Enumerated(STRING)
    private SurveyStatusEnum status;
}
