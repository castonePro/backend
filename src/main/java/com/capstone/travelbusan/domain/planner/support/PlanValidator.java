package com.capstone.travelbusan.domain.planner.support;

import com.capstone.travelbusan.domain.planner.dto.GeneratedCourseDto;
import com.capstone.travelbusan.domain.planner.dto.GeneratedPlanDto;
import com.capstone.travelbusan.domain.planner.dto.LlmCourseDraft;
import com.capstone.travelbusan.domain.planner.dto.LlmPlanDraft;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM이 내놓은 일정 초안을 검증하고 DB 사실값으로 보강한다.
 *
 * <p>여기가 환각 방어선이다. 후보 목록(=RAG가 실제로 DB에서 뽑은 장소)에 없는
 * place_id는 통과하지 못한다. 장소명·좌표·운영시간도 모델 출력이 아니라
 * 후보의 DB 값으로 덮어쓰므로, 모델이 정할 수 있는 건 "어떤 장소를 몇 시에
 * 얼마나, 어떤 순서로"뿐이다.
 *
 * <p>Spring 의존성이 없어서 단위 테스트에서 그대로 호출할 수 있다.
 */
@Slf4j
public final class PlanValidator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String DEFAULT_TIME = "09:00";
    private static final int DEFAULT_DURATION_MINUTES = 60;

    private PlanValidator() {}

    public static GeneratedPlanDto validateAndEnrich(LlmPlanDraft draft,
                                                     List<PlaceCandidate> candidates,
                                                     String fallbackStartDate) {
        if (draft == null) {
            throw new IllegalStateException("AI 응답이 비어 있습니다.");
        }

        Map<Long, PlaceCandidate> byId = new LinkedHashMap<>();
        for (PlaceCandidate c : candidates) {
            byId.put(c.placeId(), c);
        }

        List<GeneratedCourseDto> courses = new ArrayList<>();
        List<LlmCourseDraft> drafts = draft.generated_courses();
        int rejected = 0;

        if (drafts != null) {
            for (LlmCourseDraft c : drafts) {
                PlaceCandidate place = byId.get(c.place_id());
                if (place == null) {
                    rejected++;
                    log.warn("후보에 없는 place_id={} 코스를 제거했습니다. (환각 가능성)", c.place_id());
                    continue;
                }
                courses.add(new GeneratedCourseDto(
                        Math.max(1, c.day_number()),
                        normalizeTime(c.start_time()),
                        c.duration_minutes() > 0 ? c.duration_minutes() : DEFAULT_DURATION_MINUTES,
                        place.title(),
                        place.placeId(),
                        place.latitude(),
                        place.longitude(),
                        (c.category_type() != null) ? c.category_type() : List.of(),
                        place.useTime(),
                        (c.description() != null) ? c.description() : ""));
            }
        }

        if (rejected > 0) {
            log.warn("검증에서 제거된 코스 {}건 / 전체 {}건", rejected, drafts.size());
        }
        if (courses.isEmpty()) {
            throw new IllegalStateException("생성된 일정에 유효한 장소가 없습니다. 조건을 바꿔 다시 시도해 주세요.");
        }

        // 저장 시 sort_order를 리스트 인덱스로 매기므로, 순서 자체가 데이터다.
        courses.sort(Comparator
                .comparingInt(GeneratedCourseDto::day_number)
                .thenComparing(GeneratedCourseDto::start_time));

        String startDate = normalizeDate(draft.start_date(), fallbackStartDate);
        String endDate = normalizeDate(draft.end_date(), startDate);

        return new GeneratedPlanDto(
                blankTo(draft.title(), "부산 여행"),
                blankTo(draft.region(), "부산광역시"),
                startDate,
                endDate,
                courses);
    }

    /** "9:00", "09:00:00" 등을 LocalTime이 파싱 가능한 "HH:mm"으로 맞춘다. */
    static String normalizeTime(String raw) {
        if (raw == null || raw.isBlank()) return DEFAULT_TIME;
        String[] parts = raw.trim().split(":");
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = (parts.length > 1) ? Integer.parseInt(parts[1].trim()) : 0;
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return DEFAULT_TIME;
            return String.format("%02d:%02d", hour, minute);
        } catch (Exception e) {
            log.warn("start_time 파싱 실패, 기본값으로 대체합니다: '{}'", raw);
            return DEFAULT_TIME;
        }
    }

    static String normalizeDate(String raw, String fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return LocalDate.parse(raw.trim(), DATE_FMT).format(DATE_FMT);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패, 기본값으로 대체합니다: '{}'", raw);
            return fallback;
        }
    }

    private static String blankTo(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
