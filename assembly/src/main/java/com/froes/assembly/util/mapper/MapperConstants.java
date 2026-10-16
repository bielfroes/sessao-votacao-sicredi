package com.froes.assembly.util.mapper;

import org.mapstruct.factory.Mappers;

public class MapperConstants {

    private MapperConstants() {
    }

    public static final ElectorMapper electorMapper = Mappers.getMapper(ElectorMapper.class);
}
