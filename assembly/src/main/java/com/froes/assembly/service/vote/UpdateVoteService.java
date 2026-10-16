package com.froes.assembly.service.vote;

import com.froes.assembly.dto.VoteDTO;
import com.froes.assembly.dto.VoteUpdateDTO;
import com.froes.assembly.entity.Vote;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.froes.assembly.util.mapper.MapperConstants.voteMapper;
import static com.froes.assembly.util.database.DatabaseConstraintUtils.isUniqueViolation;

@Service
@RequiredArgsConstructor
public class UpdateVoteService {

    private final VoteValidationService validationService;
    private final VoteQueryService queryService;

    @Transactional
    public VoteDTO execute(VoteUpdateDTO request) {
        Vote vote = queryService.findById(request.id());
        validationService.validate(request.surveyId(), request.electorId(), vote.getId());
        Vote updatedVote = vote.withSurveyId(request.surveyId())
                .withElectorId(request.electorId())
                .withApproval(request.approval() == com.froes.assembly.enums.VoteChoice.SIM);
        try {
            return voteMapper.buildVoteDTO(queryService.saveAndFlush(updatedVote));
        } catch (DataIntegrityViolationException exception) {
            if (!isUniqueViolation(exception)) {
                throw exception;
            }
            throw queryService.duplicateVote(request.surveyId(), request.electorId());
        }
    }

}
