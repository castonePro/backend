package com.capstone.travelbusan.domain.planner;

import com.capstone.travelbusan.domain.planner.dto.GeneratedCourseDto;
import com.capstone.travelbusan.domain.planner.dto.GeneratedPlanDto;
import com.capstone.travelbusan.domain.planner.dto.LlmPatchOperation;
import com.capstone.travelbusan.domain.planner.support.PlaceCandidate;
import com.capstone.travelbusan.domain.planner.support.PlanPatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * patch 적용 회귀 테스트.
 *
 * <p>여기서 지키려는 계약이 두 개다.
 * <ol>
 *   <li>모든 연산의 day/position은 <b>원본</b> 기준이다. 앞선 연산 때문에 번호가 밀리지 않는다.
 *       LLM이 번호 시프트를 계산하게 두면 반드시 틀린다.</li>
 *   <li>요청하지 않은 날은 그대로다. 이게 깨지면 사용자가 "내가 건드리지도 않은 게 왜 바뀌지"를 겪는다.</li>
 * </ol>
 */
class PlanPatcherTest {

    private static PlaceCandidate candidate(long id, String title) {
        return new PlaceCandidate(id, title, "부산 어딘가", "자연", "관광지", "",
                "09:00 - 18:00", 35.1 + id / 1000.0, 129.1 + id / 1000.0, "청크");
    }

    private static Map<Long, PlaceCandidate> candidates() {
        Map<Long, PlaceCandidate> map = new LinkedHashMap<>();
        for (long id = 1; id <= 5; id++) {
            map.put(id, candidate(id, "장소" + id));
        }
        return map;
    }

    private static GeneratedCourseDto course(int day, String time, long placeId) {
        return new GeneratedCourseDto(day, time, 120, "장소" + placeId, placeId,
                35.0, 129.0, List.of("자연"), "09:00 - 18:00", "설명" + placeId);
    }

    /** 1일차 3코스(09:00/12:00/15:00) + 2일차 1코스. */
    private static GeneratedPlanDto plan() {
        return new GeneratedPlanDto("부산 여행", "부산광역시", "2026-09-18", "2026-09-19",
                List.of(
                        course(1, "09:00", 1L),
                        course(1, "12:00", 2L),
                        course(1, "15:00", 3L),
                        course(2, "10:00", 4L)));
    }

    private static LlmPatchOperation op(String kind, int day, int position, Long placeId,
                                        String time, Integer duration, String description) {
        return new LlmPatchOperation(kind, day, position, placeId, time, duration, description);
    }

    private static List<Long> placeIdsOf(GeneratedPlanDto p) {
        return p.generated_courses().stream().map(GeneratedCourseDto::place_id).toList();
    }

    @Test
    @DisplayName("여러 remove의 position은 원본 기준이라 서로 밀리지 않는다")
    void multipleRemovesUseOriginalPositions() {
        PlanPatcher.PatchOutcome outcome = PlanPatcher.apply(plan(), List.of(
                op("remove", 1, 1, null, null, null, null),
                op("remove", 1, 3, null, null, null, null)
        ), candidates());

        assertThat(outcome.applied()).isEqualTo(2);
        assertThat(outcome.rejected()).isZero();
        // 1일차는 원래 2번만 남고, 2일차는 손대지 않았다
        assertThat(placeIdsOf(outcome.plan())).containsExactly(2L, 4L);
    }

    @Test
    @DisplayName("add와 remove가 같은 자리를 가리켜도 원본 기준으로 각각 적용된다")
    void addAndRemoveAtSamePosition() {
        PlanPatcher.PatchOutcome outcome = PlanPatcher.apply(plan(), List.of(
                op("add", 1, 2, 5L, "10:30", 90, "새로 넣은 코스"),
                op("remove", 1, 2, null, null, null, null)
        ), candidates());

        assertThat(outcome.applied()).isEqualTo(2);
        // 09:00 장소1 → 10:30 장소5(신규) → 15:00 장소3, 그리고 2일차 유지
        assertThat(placeIdsOf(outcome.plan())).containsExactly(1L, 5L, 3L, 4L);
        assertThat(outcome.changedPlaceIds()).containsExactly(5L);
    }

    @Test
    @DisplayName("요청한 코스만 바뀌고 나머지 날은 그대로다")
    void updateTouchesOnlyTheTargetedCourse() {
        GeneratedPlanDto before = plan();

        PlanPatcher.PatchOutcome outcome = PlanPatcher.apply(before, List.of(
                op("update", 1, 3, null, "18:00", 180, null)
        ), candidates());

        List<GeneratedCourseDto> after = outcome.plan().generated_courses();
        assertThat(placeIdsOf(outcome.plan())).containsExactly(1L, 2L, 3L, 4L);

        GeneratedCourseDto updated = after.get(2);
        assertThat(updated.start_time()).isEqualTo("18:00");
        assertThat(updated.duration_minutes()).isEqualTo(180);
        assertThat(updated.description()).isEqualTo("설명3");   // null은 유지

        // 2일차는 객체 단위로 동일
        assertThat(after.get(3)).isEqualTo(before.generated_courses().get(3));
    }

    @Test
    @DisplayName("update로 장소를 교체하면 좌표·운영시간·카테고리가 DB 후보 값으로 갈린다")
    void updateSwapsPlaceWithDatabaseValues() {
        PlanPatcher.PatchOutcome outcome = PlanPatcher.apply(plan(), List.of(
                op("update", 1, 1, 5L, null, null, null)
        ), candidates());

        GeneratedCourseDto swapped = outcome.plan().generated_courses().get(0);
        PlaceCandidate expected = candidates().get(5L);

        assertThat(swapped.place_id()).isEqualTo(5L);
        assertThat(swapped.place()).isEqualTo(expected.title());
        assertThat(swapped.latitude()).isEqualTo(expected.latitude());
        assertThat(swapped.longitude()).isEqualTo(expected.longitude());
        assertThat(swapped.operating_hours()).isEqualTo(expected.useTime());
        assertThat(swapped.start_time()).isEqualTo("09:00");   // 시간은 유지
    }

    @Test
    @DisplayName("후보에 없는 place_id를 넣으려는 연산은 거절되고 일정은 그대로다")
    void rejectsHallucinatedPlaceIdInPatch() {
        GeneratedPlanDto before = plan();

        PlanPatcher.PatchOutcome outcome = PlanPatcher.apply(before, List.of(
                op("add", 1, 1, 99999L, "08:00", 60, "없는 장소")
        ), candidates());

        assertThat(outcome.applied()).isZero();
        assertThat(outcome.rejected()).isEqualTo(1);
        assertThat(outcome.plan().generated_courses()).isEqualTo(before.generated_courses());
    }

    @Test
    @DisplayName("범위를 벗어난 position이나 알 수 없는 op은 버려진다")
    void rejectsOutOfRangeAndUnknownOps() {
        PlanPatcher.PatchOutcome outcome = PlanPatcher.apply(plan(), List.of(
                op("remove", 1, 99, null, null, null, null),
                op("shuffle", 1, 1, null, null, null, null),
                op("update", 9, 1, null, "08:00", null, null)
        ), candidates());

        assertThat(outcome.applied()).isZero();
        assertThat(outcome.rejected()).isEqualTo(3);
    }

    @Test
    @DisplayName("모든 코스를 지우려 하면 일정을 비우지 않고 막는다")
    void refusesToEmptyThePlan() {
        assertThatThrownBy(() -> PlanPatcher.apply(plan(), List.of(
                op("remove", 1, 1, null, null, null, null),
                op("remove", 1, 2, null, null, null, null),
                op("remove", 1, 3, null, null, null, null),
                op("remove", 2, 1, null, null, null, null)
        ), candidates()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("빈 일정");
    }

    @Test
    @DisplayName("없는 일차에 add하면 새 일차로 붙는다")
    void addCreatesNewDay() {
        PlanPatcher.PatchOutcome outcome = PlanPatcher.apply(plan(), List.of(
                op("add", 3, 1, 5L, "11:00", 60, "3일차 추가")
        ), candidates());

        assertThat(outcome.applied()).isEqualTo(1);
        List<GeneratedCourseDto> courses = outcome.plan().generated_courses();
        GeneratedCourseDto last = courses.get(courses.size() - 1);
        assertThat(last.day_number()).isEqualTo(3);
        assertThat(last.place_id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("빈 연산 목록이면 일정이 그대로 유지된다")
    void emptyOperationsKeepPlanIntact() {
        GeneratedPlanDto before = plan();
        PlanPatcher.PatchOutcome outcome = PlanPatcher.apply(before, List.of(), candidates());

        assertThat(outcome.applied()).isZero();
        assertThat(outcome.plan().generated_courses()).isEqualTo(before.generated_courses());
    }
}
