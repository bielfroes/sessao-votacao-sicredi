package com.froes.assembly.dto;


import com.froes.assembly.enums.ElectorStatusEnum;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.With;

@Builder
@With
public record ElectorUpdateDTO(@NotNull Long id, @NotNull String cpf, ElectorStatusEnum status) {}
