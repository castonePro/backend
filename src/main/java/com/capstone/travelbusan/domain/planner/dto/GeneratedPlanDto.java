package com.capstone.travelbusan.domain.planner.dto;

import java.util.List;

/** 검증이 끝난 일정. 프론트 GeneratedPlan 타입과 형태가 같다. */
public record GeneratedPlanDto(
        String title,
        String region,
        String start_date,
        String end_date,
        List<GeneratedCourseDto> generated_courses
) {}
