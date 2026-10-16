package com.froes.assembly.service;

import com.froes.assembly.dto.ResultDTO;
import com.froes.assembly.dto.VoteDTO;
import com.froes.assembly.dto.VoteRequestDTO;
import com.froes.assembly.dto.VoteUpdateDTO;
import com.froes.assembly.entity.Vote;
import com.froes.assembly.helper.MessageHelper;
import com.froes.assembly.repository.VoteRepository;
import com.froes.assembly.service.result.VotingResultService;
import com.froes.assembly.service.elector.ElectorService;
import com.froes.assembly.service.survey.SurveyService;
import com.froes.assembly.service.session.VotingSessionService;
import com.froes.assembly.service.vote.RegisterVoteService;
import com.froes.assembly.service.vote.UpdateVoteService;
import com.froes.assembly.service.vote.VoteQueryService;
import com.froes.assembly.service.vote.VoteValidationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VoteService {

    private final RegisterVoteService registerVoteService;
    private final UpdateVoteService updateVoteService;
    private final VoteQueryService voteQueryService;
    private final VotingResultService votingResultService;

    public VoteService(
            VoteRepository repository,
            MessageHelper messageHelper,
            ElectorService electorService,
            SurveyService surveyService,
            VotingSessionService votingSessionService) {
        VoteValidationService validationService = new VoteValidationService(
                repository, messageHelper, electorService, surveyService, votingSessionService);
        this.voteQueryService = new VoteQueryService(repository, messageHelper);
        this.registerVoteService = new RegisterVoteService(validationService, voteQueryService);
        this.updateVoteService = new UpdateVoteService(validationService, voteQueryService);
        this.votingResultService = new VotingResultService(repository, surveyService);
    }

    public VoteDTO create(VoteRequestDTO request) {
        return registerVoteService.execute(request);
    }

    public VoteDTO update(VoteUpdateDTO request) {
        return updateVoteService.execute(request);
    }

    public Vote findById(Long id) {
        return voteQueryService.findById(id);
    }

    public VoteDTO findDTOById(Long id) {
        return voteQueryService.findDTOById(id);
    }

    public void delete(Long id) {
        voteQueryService.delete(id);
    }

    public Page<VoteDTO> findAll(Pageable pageable) {
        return voteQueryService.findAll(pageable);
    }

    public ResultDTO surveyResult(Long surveyId) {
        return votingResultService.findBySurveyId(surveyId);
    }

    public List<ResultDTO> assemblyResult() {
        return votingResultService.findAll();
    }
}
