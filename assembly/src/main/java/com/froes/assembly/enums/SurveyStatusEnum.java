package com.froes.assembly.enums;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.froes.assembly.enums.serializer.EnumSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@JsonSerialize(using = EnumSerializer.class)
@AllArgsConstructor
@Getter
public enum SurveyStatusEnum implements EnumDescription {

    NOT_STARTED("Aguardando abertura da votação"),
    OPEN("Votação aberta"),
    CLOSED("Votação encerrada");

    private final String description;

}
