package com.froes.assembly.dto;

import com.froes.assembly.enums.ElectorStatusEnum;
import jakarta.validation.constraints.NotNull;

public record ElectorDTO (Long id, @NotNull String cpf, ElectorStatusEnum status) {}


