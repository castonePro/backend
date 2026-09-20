package com.capstone.travelbusan.domain.planner.dto;

import java.util.List;

/**
 * LLM 원본 출력. 서버 검증을 거쳐 {@link GeneratedPlanDto}로 변환된다.
 *
 * <p>reply는 사용자에게 보여줄 한두 문장이다. 일정(데이터)과 말(자연어)을 같은
 * 스키마 안에서 분리해 받으면, 모델이 설명을 덧붙여도 JSON이 깨지지 않는다.
 */
public record LlmPlanDraft(
        String reply,
        String title,
        String region,
        String start_date,
        String end_date,
        List<LlmCourseDraft> generated_courses
) {}
