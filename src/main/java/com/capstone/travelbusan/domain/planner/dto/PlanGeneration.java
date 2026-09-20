package com.capstone.travelbusan.domain.planner.dto;

/**
 * 일정 생성 결과 묶음.
 *
 * @param plan             검증이 끝난 일정
 * @param reply            사용자에게 보여줄 한두 문장 (모델이 사용자 언어로 작성)
 * @param promptTokens     이 생성에 쓴 입력 토큰
 * @param completionTokens 이 생성에 쓴 출력 토큰
 */
public record PlanGeneration(
        GeneratedPlanDto plan,
        String reply,
        int promptTokens,
        int completionTokens
) {}
