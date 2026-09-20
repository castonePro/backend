package com.capstone.travelbusan.domain.planner.support;

/**
 * 사용자 발화의 의도.
 *
 * <p>매 턴 무조건 RAG 30개 + 전체 일정 재생성을 돌리면 비용이 턴 수에 비례해 터진다.
 * 저렴한 1차 호출로 의도를 나눠서, 실제로 필요한 파이프라인만 태운다.
 */
public enum PlannerIntent {

    /** 처음부터 새 일정을 만든다. RAG 검색 + 전체 생성. */
    NEW_PLAN,

    /** 기존 일정을 고친다. 전체 재생성 대신 patch 연산만 받는다. */
    MODIFY,

    /** 일정은 그대로 두고 질문에만 답한다. */
    ASK,

    /** 여행과 무관한 요청. 정중히 거절한다. */
    OUT_OF_SCOPE;

    public static PlannerIntent from(String raw, PlannerIntent fallback) {
        if (raw == null) return fallback;
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
