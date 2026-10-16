package com.froes.assembly.util.mapper;


import com.froes.assembly.dto.ElectorDTO;
import com.froes.assembly.dto.ElectorRequestDTO;
import com.froes.assembly.entity.Elector;
import org.mapstruct.Mapper;

@Mapper
public interface ElectorMapper {
    Elector buildElector(ElectorRequestDTO requestDTO);

    ElectorDTO buildElectorDTO(Elector elector);
}
