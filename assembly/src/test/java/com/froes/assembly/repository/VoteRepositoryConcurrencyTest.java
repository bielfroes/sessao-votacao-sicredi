package com.froes.assembly.repository;

import com.froes.assembly.entity.Elector;
import com.froes.assembly.entity.Survey;
import com.froes.assembly.entity.Vote;
import com.froes.assembly.enums.ElectorStatusEnum;
import com.froes.assembly.enums.SurveyStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class VoteRepositoryConcurrencyTest {

    @Autowired
    private ElectorRepository electorRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentVotesFromTheSameElectorPersistOnlyOneVote() throws Exception {
        Elector elector = electorRepository.saveAndFlush(Elector.builder()
                .cpf("67953008033")
                .status(ElectorStatusEnum.ABLE_TO_VOTE)
                .build());
        Survey survey = surveyRepository.saveAndFlush(Survey.builder()
                .question("Approve the proposal?")
                .endTime(LocalDateTime.now().plusMinutes(1))
                .createdAt(OffsetDateTime.now())
                .status(SurveyStatusEnum.OPEN)
                .build());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> persistVote(ready, start, survey, elector, true));
            Future<Boolean> second = executor.submit(() -> persistVote(ready, start, survey, elector, false));

            ready.await();
            start.countDown();

            int persisted = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, persisted);
            var counts = voteRepository.countVotesBySurveyId(survey.getId());
            assertEquals(1L, counts.approves() + counts.reproves());
        }
    }

    @Test
    void countVotesBySurveyIdsGroupsVotesBySurvey() {
        Survey survey = surveyRepository.saveAndFlush(Survey.builder()
                .question("Approve the proposal?")
                .endTime(LocalDateTime.now().plusMinutes(1))
                .createdAt(OffsetDateTime.now())
                .status(SurveyStatusEnum.OPEN)
                .build());
        Elector firstElector = electorRepository.saveAndFlush(Elector.builder()
                .cpf("67953008033")
                .status(ElectorStatusEnum.ABLE_TO_VOTE)
                .build());
        Elector secondElector = electorRepository.saveAndFlush(Elector.builder()
                .cpf("52998224725")
                .status(ElectorStatusEnum.ABLE_TO_VOTE)
                .build());
        voteRepository.saveAndFlush(Vote.builder()
                .surveyId(survey.getId()).electorId(firstElector.getId()).approval(true).build());
        voteRepository.saveAndFlush(Vote.builder()
                .surveyId(survey.getId()).electorId(secondElector.getId()).approval(false).build());

        var counts = voteRepository.countVotesBySurveyIds(List.of(survey.getId()));

        assertEquals(1, counts.size());
        assertEquals(new SurveyVoteCounts(survey.getId(), 1L, 1L), counts.getFirst());
    }

    private boolean persistVote(CountDownLatch ready, CountDownLatch start, Survey survey,
                                Elector elector, boolean approval) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            voteRepository.saveAndFlush(Vote.builder()
                    .surveyId(survey.getId())
                    .electorId(elector.getId())
                    .approval(approval)
                    .build());
            return true;
        } catch (DataIntegrityViolationException duplicateVote) {
            return false;
        }
    }
}
