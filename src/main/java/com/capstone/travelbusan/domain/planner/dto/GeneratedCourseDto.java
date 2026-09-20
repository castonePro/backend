package com.capstone.travelbusan.domain.planner.dto;

import java.util.List;

/**
 * 검증·보강이 끝난 코스 한 건 (클라이언트 응답용).
 *
 * <p>place / latitude / longitude / operating_hours는 LLM 출력이 아니라
 * place_id로 조회한 DB 값이다. 따라서 존재하지 않는 장소나 틀린 좌표가 나올 수 없다.
 */
public record GeneratedCourseDto(
        int day_number,
        String start_time,
        int duration_minutes,
        String place,
        Long place_id,
        Double latitude,
        Double longitude,
        List<String> category_type,
        String operating_hours,
        String description
) {}
