package com.froes.assembly.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.froes.assembly.service.outbox.VotingResultOutboxService;
import com.froes.assembly.service.session.VotingSessionService;


@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {
    private final VotingSessionService votingSessionService;
    private final VotingResultOutboxService votingResultOutboxService;

    @Scheduled(fixedDelayString = "${app.voting.expiration-check-delay-ms:60000}")
    public void updateSurveyStatus() {
        votingSessionService.closeExpiredSessions();
        votingResultOutboxService.publishPending();
    }
}
