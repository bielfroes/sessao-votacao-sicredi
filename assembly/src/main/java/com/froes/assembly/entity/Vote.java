package com.froes.assembly.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "vote")
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "survey_id", nullable = false)
    private Long surveyId;
    @Column(name = "elector_id", nullable = false)
    private Long electorId;
    @Column(nullable = false)
    private Boolean approval;
}
