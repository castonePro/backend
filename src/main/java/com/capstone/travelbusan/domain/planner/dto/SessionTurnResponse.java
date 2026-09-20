package com.capstone.travelbusan.domain.planner.dto;

import java.util.List;

/**
 * 한 턴의 결과.
 *
 * @param sessionId        세션 식별자
 * @param intent           이 턴이 어떻게 분류됐는지 (NEW_PLAN / MODIFY / ASK / OUT_OF_SCOPE)
 * @param reply            사용자에게 보여줄 자연어 답변
 * @param plan             갱신된 현재 일정. 일정이 바뀌지 않는 턴(ASK 등)에서도 최신본을 그대로 돌려준다
 * @param changedPlaceIds  이번 턴에 추가·수정된 코스의 place_id.
 *                         프론트가 해당 카드에 "수정됨" 표시를 붙이는 데 쓴다
 * @param turnCount        지금까지 진행된 턴 수
 * @param remainingTurns   이 세션에서 남은 턴 수
 */
public record SessionTurnResponse(
        String sessionId,
        String intent,
        String reply,
        GeneratedPlanDto plan,
        List<Long> changedPlaceIds,
        int turnCount,
        int remainingTurns
) {}
