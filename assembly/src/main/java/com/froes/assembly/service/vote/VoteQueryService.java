package com.froes.assembly.service.vote;

import com.froes.assembly.dto.VoteDTO;
import com.froes.assembly.entity.Vote;
import com.froes.assembly.helper.MessageHelper;
import com.froes.assembly.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static com.froes.assembly.exception.ErrorCodeEnum.ERROR_VOTE_NOT_FOUND;
import static com.froes.assembly.util.mapper.MapperConstants.voteMapper;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteQueryService {

    private final VoteRepository repository;
    private final MessageHelper messageHelper;

    public Vote findById(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error(messageHelper.get(ERROR_VOTE_NOT_FOUND, id));
            return new ResponseStatusException(NOT_FOUND, messageHelper.get(ERROR_VOTE_NOT_FOUND, id));
        });
    }

    public VoteDTO findDTOById(Long id) {
        return voteMapper.buildVoteDTO(findById(id));
    }

    public void delete(Long id) {
        repository.delete(findById(id));
    }

    public Page<VoteDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(voteMapper::buildVoteDTO);
    }

    public Vote saveAndFlush(Vote vote) {
        return repository.saveAndFlush(vote);
    }

    public ResponseStatusException duplicateVote(Long surveyId, Long electorId) {
        return new ResponseStatusException(CONFLICT, "Eleitor " + electorId
                + " já votou na pauta " + surveyId + ".");
    }
}
