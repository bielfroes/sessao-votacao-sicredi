package com.froes.assembly.entity;

import com.froes.assembly.enums.ElectorStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "elector")
public class Elector {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 11)
    private String cpf;
    @Enumerated(STRING)
    private ElectorStatusEnum status;

}
