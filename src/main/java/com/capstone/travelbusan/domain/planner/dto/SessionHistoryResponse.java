package com.capstone.travelbusan.domain.planner.dto;

import java.util.List;

/**
 * 세션 전체 상태. 새로고침해도 대화와 일정이 살아나도록 프론트가 이걸로 복구한다.
 */
public record SessionHistoryResponse(
        String sessionId,
        String locale,
        List<SessionMessageDto> messages,
        GeneratedPlanDto plan,
        int turnCount,
        int remainingTurns
) {}
