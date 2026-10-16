package com.froes.assembly.repository;

import com.froes.assembly.entity.VotingResultOutbox;
import com.froes.assembly.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

public interface VotingResultOutboxRepository extends JpaRepository<VotingResultOutbox, Long> {

    boolean existsByEventKey(String eventKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event from VotingResultOutbox event
            where event.status = :status
            order by event.createdAt
            """)
    List<VotingResultOutbox> findPending(@Param("status") OutboxStatus status);
}
