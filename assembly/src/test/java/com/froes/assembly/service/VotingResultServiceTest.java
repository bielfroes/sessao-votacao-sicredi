package com.froes.assembly.service;

import com.froes.assembly.dto.SurveyDTO;
import com.froes.assembly.enums.ResultStatusEnum;
import com.froes.assembly.enums.SurveyStatusEnum;
import com.froes.assembly.repository.VoteCounts;
import com.froes.assembly.repository.VoteRepository;
import com.froes.assembly.service.result.VotingResultService;
import com.froes.assembly.service.survey.SurveyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class VotingResultServiceTest {

    @InjectMocks
    private VotingResultService service;

    @Mock
    private VoteRepository repository;

    @Mock
    private SurveyService surveyService;

    @Test
    void findBySurveyId_reportsNotStartedBeforeSessionOpens() {
        SurveyDTO survey = new SurveyDTO(1L, null, "Pauta aguardando votação",
                SurveyStatusEnum.NOT_STARTED, OffsetDateTime.now(), 1L);
        when(surveyService.findDTOById(1L)).thenReturn(survey);
        when(repository.countVotesBySurveyId(1L)).thenReturn(new VoteCounts(0L, 0L));

        var result = service.findBySurveyId(1L);

        assertEquals(ResultStatusEnum.NOT_STARTED, result.status());
    }
}
