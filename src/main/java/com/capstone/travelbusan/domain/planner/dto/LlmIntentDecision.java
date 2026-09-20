package com.capstone.travelbusan.domain.planner.dto;

/**
 * 1차 분류 호출의 결과.
 *
 * @param intent NEW_PLAN / MODIFY / ASK / OUT_OF_SCOPE
 * @param reply  ASK·OUT_OF_SCOPE일 때 그대로 사용자에게 보여줄 답변.
 *               NEW_PLAN·MODIFY일 때는 비워도 된다(2차 호출이 답변을 만든다).
 */
public record LlmIntentDecision(String intent, String reply) {}
