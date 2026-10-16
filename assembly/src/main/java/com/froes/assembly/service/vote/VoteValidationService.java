package com.froes.assembly.service.vote;

import com.froes.assembly.entity.Elector;
import com.froes.assembly.entity.Survey;
import com.froes.assembly.enums.ElectorStatusEnum;
import com.froes.assembly.helper.MessageHelper;
import com.froes.assembly.repository.VoteRepository;
import com.froes.assembly.service.elector.ElectorService;
import com.froes.assembly.service.survey.SurveyService;
import com.froes.assembly.service.session.VotingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static com.froes.assembly.exception.ErrorCodeEnum.ERROR_ELECTOR_ALREADY_VOTED_FOR_THIS_SURVEY;
import static com.froes.assembly.exception.ErrorCodeEnum.ERROR_ELECTOR_UNABLE_TO_VOTE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteValidationService {

    private final VoteRepository repository;
    private final MessageHelper messageHelper;
    private final ElectorService electorService;
    private final SurveyService surveyService;
    private final VotingSessionService votingSessionService;

    public void validate(Long surveyId, Long electorId) {
        validate(surveyId, electorId, null);
    }

    public void validate(Long surveyId, Long electorId, Long excludedVoteId) {
        surveyService.findById(surveyId);
        Elector elector = electorService.findById(electorId);

        if (electorService.statusForVoting(elector) != ElectorStatusEnum.ABLE_TO_VOTE) {
            log.warn("Voto rejeitado: eleitor {} não está elegível para a pauta {}", electorId, surveyId);
            throw new ResponseStatusException(BAD_REQUEST,
                    messageHelper.get(ERROR_ELECTOR_UNABLE_TO_VOTE, electorId));
        }

        boolean alreadyVoted = excludedVoteId == null
                ? repository.existsBySurveyIdAndElectorId(surveyId, electorId)
                : repository.existsBySurveyIdAndElectorIdAndIdNot(surveyId, electorId, excludedVoteId);
        if (alreadyVoted) {
            throw duplicateVote(surveyId, electorId);
        }

        votingSessionService.requireOpen(surveyId);
    }

    private ResponseStatusException duplicateVote(Long surveyId, Long electorId) {
        log.warn("Voto duplicado rejeitado para o eleitor {} na pauta {}", electorId, surveyId);
        return new ResponseStatusException(CONFLICT, messageHelper.get(
                ERROR_ELECTOR_ALREADY_VOTED_FOR_THIS_SURVEY, electorId, surveyId));
    }
}
