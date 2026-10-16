package com.froes.assembly.util.creator;


import com.froes.assembly.dto.VoteDTO;
import com.froes.assembly.dto.VoteRequestDTO;
import com.froes.assembly.dto.VoteUpdateDTO;
import com.froes.assembly.entity.Vote;
import com.froes.assembly.enums.VoteChoice;

import static com.froes.assembly.util.PodamFactory.podam;
import static com.froes.assembly.util.creator.ElectorCreator.elector;
import static com.froes.assembly.util.creator.SurveyCreator.survey;
import static com.froes.assembly.util.mapper.MapperConstants.voteMapper;

public class VoteCreator {

    public final static String VALID_CPF = "679.530.080-33";
    public final static String INVALID_CPF = "679.530.080-37";


    public static final Vote vote = podam.manufacturePojo(Vote.class);
    public static final VoteDTO voteDTO = voteMapper.buildVoteDTO(vote);

    public static VoteRequestDTO createVoteRequestDTO() {
        return VoteRequestDTO.builder()
                .electorId(elector.getId())
                .surveyId(survey.getId())
                .approval(Boolean.TRUE.equals(vote.getApproval()) ? VoteChoice.SIM : VoteChoice.NAO)
                .build();
    }


    public static VoteUpdateDTO voteUpdateDTO() {
        return VoteUpdateDTO.builder()
                .electorId(elector.getId())
                .surveyId(survey.getId())
                .approval(Boolean.TRUE.equals(vote.getApproval()) ? VoteChoice.SIM : VoteChoice.NAO)
                .build();
    }
}
