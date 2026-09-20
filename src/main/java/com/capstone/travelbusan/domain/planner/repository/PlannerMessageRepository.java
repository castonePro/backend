package com.capstone.travelbusan.domain.planner.repository;

import com.capstone.travelbusan.domain.planner.entity.PlannerMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlannerMessageRepository extends JpaRepository<PlannerMessage, UUID> {

    /** 히스토리 복구용 — 오래된 순. */
    List<PlannerMessage> findBySession_SessionIdOrderBySeqAsc(UUID sessionId);

    /** 프롬프트에 넣을 최근 N턴 — 최신 순으로 뽑아 호출부에서 뒤집는다. */
    List<PlannerMessage> findTop12BySession_SessionIdOrderBySeqDesc(UUID sessionId);

    int countBySession_SessionId(UUID sessionId);
}
