package com.froes.assembly.repository;

import com.froes.assembly.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long>, JpaSpecificationExecutor<Vote> {
    List<Vote> findAllBySurveyIdAndElectorId(Long surveyId, Long electorId);
    boolean existsBySurveyIdAndElectorId(Long surveyId, Long electorId);
    boolean existsBySurveyIdAndElectorIdAndIdNot(Long surveyId, Long electorId, Long id);

    @Query("""
            SELECT new com.froes.assembly.repository.VoteCounts(
                COUNT(CASE WHEN vote.approval = true THEN vote.id END),
                COUNT(CASE WHEN vote.approval = false THEN vote.id END))
            FROM Vote vote
            WHERE vote.surveyId = :surveyId
            """)
    VoteCounts countVotesBySurveyId(@Param("surveyId") Long surveyId);

    @Query("""
            SELECT new com.froes.assembly.repository.SurveyVoteCounts(
                vote.surveyId,
                COUNT(CASE WHEN vote.approval = true THEN vote.id END),
                COUNT(CASE WHEN vote.approval = false THEN vote.id END))
            FROM Vote vote
            WHERE vote.surveyId IN :surveyIds
            GROUP BY vote.surveyId
            """)
    List<SurveyVoteCounts> countVotesBySurveyIds(@Param("surveyIds") List<Long> surveyIds);

}
