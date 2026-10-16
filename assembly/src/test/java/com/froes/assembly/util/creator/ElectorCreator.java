package com.froes.assembly.util.creator;

import com.froes.assembly.dto.ElectorDTO;
import com.froes.assembly.dto.ElectorRequestDTO;
import com.froes.assembly.dto.ElectorUpdateDTO;
import com.froes.assembly.entity.Elector;
import com.froes.assembly.enums.ElectorStatusEnum;

import static com.froes.assembly.util.PodamFactory.podam;
import static com.froes.assembly.util.mapper.MapperConstants.electorMapper;

public class ElectorCreator {

    public final static String VALID_CPF = "679.530.080-33";
    public final static String INVALID_CPF = "679.530.080-37";


    public static final Elector elector = podam.manufacturePojo(Elector.class).withCpf(VALID_CPF)
            .withStatus(ElectorStatusEnum.ABLE_TO_VOTE);
    public static final ElectorDTO electorDTO = electorMapper.buildElectorDTO(elector);

    public static ElectorRequestDTO createElectorRequestDTO() {
        return new ElectorRequestDTO(elector.getCpf());
    }

    public static ElectorUpdateDTO electorUpdateDTO() {
        return ElectorUpdateDTO.builder()
                .id(elector.getId())
                .cpf(elector.getCpf())
                .status(elector.getStatus())
                .build();
    }
}
