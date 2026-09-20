package com.capstone.travelbusan.domain.planner.dto;

import java.time.LocalTime;
import java.util.List;

/**
 * 저장 요청의 코스 한 건.
 *
 * <p>place_id는 생성 응답에 이미 들어 있으므로 프론트가 그대로 되돌려주면 된다.
 * 이름으로 다시 조회(findFirstByTitle)하는 경로는 동명 장소와 환각 장소에 취약해서
 * place_id가 있으면 그쪽을 우선 사용한다.
 */
public record CourseSaveDto(
        int day_number,
        LocalTime start_time,
        int duration_minutes,
        String place,
        Long place_id,
        double latitude,
        double longitude,
        List<String> category_type,
        String operating_hours,
        String description
) {}
