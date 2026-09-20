package com.capstone.travelbusan.domain.planner;

import com.capstone.travelbusan.domain.planner.dto.GeneratedPlanDto;
import com.capstone.travelbusan.domain.planner.dto.LlmCourseDraft;
import com.capstone.travelbusan.domain.planner.dto.LlmPlanDraft;
import com.capstone.travelbusan.domain.planner.support.PlaceCandidate;
import com.capstone.travelbusan.domain.planner.support.PlanValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 환각 방어선 회귀 테스트.
 *
 * <p>Spring 컨텍스트도 OpenAI 키도 필요 없다. 프롬프트를 바꿨을 때
 * "없는 장소가 통과하지 않는다"는 보장이 깨지지 않았는지 여기서 확인한다.
 */
class PlanValidatorTest {

    private static final String FALLBACK_DATE = "2026-09-18";

    private static PlaceCandidate candidate(long id, String title, double lat, double lng) {
        return new PlaceCandidate(id, title, "부산광역시 어딘가", "자연", "관광지", "명소",
                "09:00 - 18:00", lat, lng, "설명 청크");
    }

    private static LlmCourseDraft course(int day, String time, long placeId) {
        return new LlmCourseDraft(day, time, 120, placeId, List.of("자연"), "산책하기");
    }

    private static LlmPlanDraft draft(List<LlmCourseDraft> courses) {
        return new LlmPlanDraft("일정을 만들었어요", "부산 여행", "부산광역시", "2026-09-18", "2026-09-19", courses);
    }

    @Test
    @DisplayName("후보 목록에 없는 place_id 코스는 제거된다")
    void rejectsHallucinatedPlaceId() {
        List<PlaceCandidate> candidates = List.of(
                candidate(1L, "태종대", 35.0531, 129.0878),
                candidate(2L, "해운대해수욕장", 35.1587, 129.1604));

        LlmPlanDraft draft = draft(List.of(
                course(1, "09:00", 1L),
                course(1, "13:00", 99999L),   // 존재하지 않는 장소
                course(2, "10:00", 2L)));

        GeneratedPlanDto plan = PlanValidator.validateAndEnrich(draft, candidates, FALLBACK_DATE);

        assertThat(plan.generated_courses()).hasSize(2);
        assertThat(plan.generated_courses())
                .extracting(c -> c.place_id())
                .containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("장소명·좌표·운영시간은 LLM 출력이 아니라 DB 후보 값으로 채워진다")
    void enrichesFromDatabaseValues() {
        List<PlaceCandidate> candidates = List.of(candidate(7L, "감천문화마을", 35.0975, 129.0106));

        GeneratedPlanDto plan = PlanValidator.validateAndEnrich(
                draft(List.of(course(1, "09:00", 7L))), candidates, FALLBACK_DATE);

        var c = plan.generated_courses().get(0);
        assertThat(c.place()).isEqualTo("감천문화마을");
        assertThat(c.latitude()).isEqualTo(35.0975);
        assertThat(c.longitude()).isEqualTo(129.0106);
        assertThat(c.operating_hours()).isEqualTo("09:00 - 18:00");
        assertThat(c.place_id()).isEqualTo(7L);
    }

    @Test
    @DisplayName("모든 코스가 환각이면 예외로 막는다 (빈 일정이 저장되지 않도록)")
    void failsWhenEveryCourseIsRejected() {
        List<PlaceCandidate> candidates = List.of(candidate(1L, "태종대", 35.0531, 129.0878));

        assertThatThrownBy(() -> PlanValidator.validateAndEnrich(
                draft(List.of(course(1, "09:00", 404L))), candidates, FALLBACK_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("유효한 장소가 없습니다");
    }

    @Test
    @DisplayName("start_time은 LocalTime이 파싱 가능한 HH:mm으로 정규화된다")
    void normalizesStartTime() {
        List<PlaceCandidate> candidates = List.of(
                candidate(1L, "A", 35.0, 129.0),
                candidate(2L, "B", 35.0, 129.0),
                candidate(3L, "C", 35.0, 129.0));

        GeneratedPlanDto plan = PlanValidator.validateAndEnrich(
                draft(List.of(
                        course(1, "9:5", 1L),
                        course(1, "14:00:00", 2L),
                        course(1, "25:99", 3L))),   // 말이 안 되는 값은 기본값으로
                candidates, FALLBACK_DATE);

        assertThat(plan.generated_courses())
                .extracting(c -> c.start_time())
                .containsExactly("09:00", "09:05", "14:00");
    }

    @Test
    @DisplayName("코스는 일차 → 시간 순으로 정렬된다 (sort_order가 리스트 순서에서 나오므로)")
    void sortsByDayThenTime() {
        List<PlaceCandidate> candidates = List.of(
                candidate(1L, "A", 35.0, 129.0),
                candidate(2L, "B", 35.0, 129.0),
                candidate(3L, "C", 35.0, 129.0));

        GeneratedPlanDto plan = PlanValidator.validateAndEnrich(
                draft(List.of(
                        course(2, "10:00", 1L),
                        course(1, "15:00", 2L),
                        course(1, "09:00", 3L))),
                candidates, FALLBACK_DATE);

        assertThat(plan.generated_courses())
                .extracting(c -> c.day_number() + " " + c.start_time())
                .containsExactly("1 09:00", "1 15:00", "2 10:00");
    }

    @Test
    @DisplayName("날짜가 비었거나 형식이 깨지면 서버 기준일로 대체된다")
    void fallsBackOnBrokenDates() {
        List<PlaceCandidate> candidates = List.of(candidate(1L, "A", 35.0, 129.0));
        LlmPlanDraft broken = new LlmPlanDraft("요약", "제목", "부산광역시", "내일", null,
                List.of(course(1, "09:00", 1L)));

        GeneratedPlanDto plan = PlanValidator.validateAndEnrich(broken, candidates, FALLBACK_DATE);

        assertThat(plan.start_date()).isEqualTo(FALLBACK_DATE);
        assertThat(plan.end_date()).isEqualTo(FALLBACK_DATE);
    }
}
