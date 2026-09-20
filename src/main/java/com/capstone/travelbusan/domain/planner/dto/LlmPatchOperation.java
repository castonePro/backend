package com.capstone.travelbusan.domain.planner.dto;

/**
 * 기존 일정에 가할 변경 한 건.
 *
 * <p>전체 일정을 다시 생성하지 않고 이 연산만 받는 이유:
 * <ul>
 *   <li>건드리지 않은 날이 바이트 단위로 그대로 유지된다 (상태 드리프트 방지)</li>
 *   <li>출력 토큰이 10분의 1 수준으로 줄어든다</li>
 *   <li>무엇이 바뀌었는지 서버가 알고 있어서 UI가 하이라이트할 수 있다</li>
 * </ul>
 *
 * @param op               "add" | "remove" | "update"
 * @param day_number       대상 일차 (1부터)
 * @param position         해당 일차 안에서의 순번 (1부터).
 *                         add는 이 자리 앞에 끼워 넣고, remove·update는 이 자리를 가리킨다.
 * @param place_id         add에서 필수. update에서 장소 자체를 바꿀 때만 사용. 그 외 null
 * @param start_time       바꿀 시각 "HH:mm". 유지하려면 null
 * @param duration_minutes 바꿀 체류 시간. 유지하려면 null
 * @param description      바꿀 설명. 유지하려면 null
 */
public record LlmPatchOperation(
        String op,
        Integer day_number,
        Integer position,
        Long place_id,
        String start_time,
        Integer duration_minutes,
        String description
) {}
