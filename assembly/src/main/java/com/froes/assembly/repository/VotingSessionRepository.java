package com.froes.assembly.repository;

import com.froes.assembly.entity.VotingSession;
import com.froes.assembly.enums.SurveyStatusEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VotingSessionRepository extends JpaRepository<VotingSession, Long> {

    boolean existsBySurveyIdAndStatus(Long surveyId, SurveyStatusEnum status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VotingSession> findFirstBySurveyIdAndStatusOrderByOpenedAtDesc(
            Long surveyId, SurveyStatusEnum status);

    Optional<VotingSession> findFirstBySurveyIdOrderByOpenedAtDesc(Long surveyId);

    @Query(value = """
            SELECT * FROM voting_session
            WHERE status = 'OPEN' AND end_time < :now
            ORDER BY end_time
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<VotingSession> lockExpiredSessions(@Param("now") OffsetDateTime now);
}
