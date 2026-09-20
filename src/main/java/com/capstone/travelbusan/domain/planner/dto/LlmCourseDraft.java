package com.capstone.travelbusan.domain.planner.dto;

import java.util.List;

/**
 * LLM이 내놓는 코스 한 건. Structured Outputs 스키마와 1:1로 대응한다.
 *
 * <p>장소명·좌표·운영시간이 없는 게 핵심이다. 그 값들은 사실(fact)이라
 * 모델이 아니라 DB가 책임진다. 모델은 "어떤 place_id를 몇 시에 얼마나"만 정한다.
 */
public record LlmCourseDraft(
        int day_number,
        String start_time,
        int duration_minutes,
        long place_id,
        List<String> category_type,
        String description
) {}
