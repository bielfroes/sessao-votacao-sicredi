package com.froes.assembly.service.vote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.froes.assembly.util.database.DatabaseConstraintUtils.isUniqueViolation;
import static com.froes.assembly.util.mapper.MapperConstants.voteMapper;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.froes.assembly.dto.VoteDTO;
import com.froes.assembly.dto.VoteRequestDTO;
import com.froes.assembly.entity.Vote;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterVoteService {

    private final VoteValidationService validationService;
    private final VoteQueryService queryService;

    @Transactional
    public VoteDTO execute(VoteRequestDTO request) {
        validationService.validate(request.surveyId(), request.electorId());
        try {
            Vote vote = queryService.saveAndFlush(voteMapper.buildVote(request));
            log.info("Voto registrado na pauta {} pelo eleitor {}", vote.getSurveyId(), vote.getElectorId());
            return voteMapper.buildVoteDTO(vote);
        } catch (DataIntegrityViolationException exception) {
            if (!isUniqueViolation(exception)) {
                throw exception;
            }
            throw queryService.duplicateVote(request.surveyId(), request.electorId());
        }
    }

}
