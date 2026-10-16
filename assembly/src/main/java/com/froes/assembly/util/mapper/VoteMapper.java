package com.froes.assembly.util.mapper;

import com.froes.assembly.dto.VoteDTO;
import com.froes.assembly.dto.VoteRequestDTO;
import com.froes.assembly.entity.Vote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface VoteMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "approval",
            expression = "java(requestDTO.approval() == com.froes.assembly.enums.VoteChoice.SIM)")
    Vote buildVote(VoteRequestDTO requestDTO);

    @Mapping(target = "choice", expression = "java(Boolean.TRUE.equals(vote.getApproval()) ? \"SIM\" : \"NAO\")")
    VoteDTO buildVoteDTO(Vote vote);
}
