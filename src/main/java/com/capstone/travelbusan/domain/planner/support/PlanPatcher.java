package com.capstone.travelbusan.domain.planner.support;

import com.capstone.travelbusan.domain.planner.dto.GeneratedCourseDto;
import com.capstone.travelbusan.domain.planner.dto.GeneratedPlanDto;
import com.capstone.travelbusan.domain.planner.dto.LlmPatchOperation;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * patch 연산을 현재 일정에 적용한다.
 *
 * <p>핵심 설계: 모든 연산의 day_number/position은 <b>연산 적용 전 원본</b> 기준이다.
 * 순차 적용하면서 인덱스가 밀리는 고전적인 버그를 피하려고, 원본 위치마다 슬롯을 두고
 * "여기는 지웠다 / 여기 앞에 끼워 넣는다 / 여기를 고쳤다"를 표시한 뒤 마지막에 한 번에
 * 재조립한다. 덕분에 LLM은 번호가 밀릴 걱정을 할 필요가 없다.
 *
 * <p>add·update의 place_id는 후보 목록으로 검증하고, 장소명·좌표·운영시간은 DB 값으로
 * 채운다. 생성 경로와 똑같은 환각 방어선이 수정 경로에도 걸린다.
 */
@Slf4j
public final class PlanPatcher {

    private PlanPatcher() {}

    /**
     * @param plan            변경이 반영된 일정
     * @param changedPlaceIds 추가·수정된 코스의 place_id. 프론트가 "수정됨" 배지를 붙이는 데 쓴다
     * @param applied         실제로 반영된 연산 수
     * @param rejected        검증에서 버려진 연산 수
     */
    public record PatchOutcome(
            GeneratedPlanDto plan,
            List<Long> changedPlaceIds,
            int applied,
            int rejected
    ) {}

    /** 원본 코스 한 자리. */
    private static final class Slot {
        GeneratedCourseDto course;
        boolean removed;
        final List<GeneratedCourseDto> insertedBefore = new ArrayList<>();

        Slot(GeneratedCourseDto course) {
            this.course = course;
        }
    }

    public static PatchOutcome apply(GeneratedPlanDto current,
                                     List<LlmPatchOperation> operations,
                                     Map<Long, PlaceCandidate> candidates) {
        if (current == null) {
            throw new IllegalStateException("수정할 일정이 없습니다.");
        }

        TreeMap<Integer, List<Slot>> byDay = new TreeMap<>();
        for (GeneratedCourseDto c : current.generated_courses()) {
            byDay.computeIfAbsent(c.day_number(), k -> new ArrayList<>()).add(new Slot(c));
        }
        TreeMap<Integer, List<GeneratedCourseDto>> appended = new TreeMap<>();

        Set<Long> changed = new LinkedHashSet<>();
        int applied = 0;
        int rejected = 0;

        for (LlmPatchOperation op : (operations == null ? List.<LlmPatchOperation>of() : operations)) {
            String reason = applyOne(op, byDay, appended, candidates, changed);
            if (reason == null) {
                applied++;
            } else {
                rejected++;
                log.warn("patch 연산을 버렸습니다: op={}, day={}, position={}, placeId={}, 이유={}",
                        op.op(), op.day_number(), op.position(), op.place_id(), reason);
            }
        }

        List<GeneratedCourseDto> rebuilt = rebuild(byDay, appended);
        if (rebuilt.isEmpty()) {
            throw new IllegalStateException("모든 코스가 삭제되어 빈 일정이 됩니다. 일정을 유지합니다.");
        }

        GeneratedPlanDto plan = new GeneratedPlanDto(
                current.title(), current.region(), current.start_date(), current.end_date(), rebuilt);

        return new PatchOutcome(plan, List.copyOf(changed), applied, rejected);
    }

    /** @return null이면 성공, 아니면 버린 이유 */
    private static String applyOne(LlmPatchOperation op,
                                   TreeMap<Integer, List<Slot>> byDay,
                                   TreeMap<Integer, List<GeneratedCourseDto>> appended,
                                   Map<Long, PlaceCandidate> candidates,
                                   Set<Long> changed) {
        if (op == null || op.op() == null) return "op이 없음";
        if (op.day_number() == null || op.day_number() < 1) return "day_number가 올바르지 않음";

        String kind = op.op().trim().toLowerCase();
        int day = op.day_number();
        int position = (op.position() != null) ? op.position() : 1;

        switch (kind) {
            case "add" -> {
                if (op.place_id() == null) return "add인데 place_id가 없음";
                PlaceCandidate place = candidates.get(op.place_id());
                if (place == null) return "후보에 없는 place_id (환각 가능성)";

                GeneratedCourseDto added = new GeneratedCourseDto(
                        day,
                        PlanValidator.normalizeTime(op.start_time()),
                        (op.duration_minutes() != null && op.duration_minutes() > 0) ? op.duration_minutes() : 60,
                        place.title(),
                        place.placeId(),
                        place.latitude(),
                        place.longitude(),
                        place.categoryTags(),
                        place.useTime(),
                        (op.description() != null) ? op.description() : "");

                List<Slot> slots = byDay.get(day);
                if (slots != null && position >= 1 && position <= slots.size()) {
                    slots.get(position - 1).insertedBefore.add(added);
                } else {
                    // 해당 일차가 없거나 맨 뒤를 가리키면 그 날의 끝에 붙인다
                    appended.computeIfAbsent(day, k -> new ArrayList<>()).add(added);
                }
                changed.add(place.placeId());
                return null;
            }
            case "remove" -> {
                Slot slot = slotAt(byDay, day, position);
                if (slot == null) return "가리키는 코스를 찾을 수 없음";
                if (slot.removed) return "이미 삭제된 코스";
                slot.removed = true;
                return null;
            }
            case "update" -> {
                Slot slot = slotAt(byDay, day, position);
                if (slot == null) return "가리키는 코스를 찾을 수 없음";
                if (slot.removed) return "삭제된 코스는 수정할 수 없음";

                GeneratedCourseDto before = slot.course;
                PlaceCandidate swapped = null;
                if (op.place_id() != null && !op.place_id().equals(before.place_id())) {
                    swapped = candidates.get(op.place_id());
                    if (swapped == null) return "후보에 없는 place_id (환각 가능성)";
                }

                slot.course = new GeneratedCourseDto(
                        before.day_number(),
                        (op.start_time() != null) ? PlanValidator.normalizeTime(op.start_time()) : before.start_time(),
                        (op.duration_minutes() != null && op.duration_minutes() > 0)
                                ? op.duration_minutes() : before.duration_minutes(),
                        (swapped != null) ? swapped.title() : before.place(),
                        (swapped != null) ? swapped.placeId() : before.place_id(),
                        (swapped != null) ? swapped.latitude() : before.latitude(),
                        (swapped != null) ? swapped.longitude() : before.longitude(),
                        (swapped != null) ? swapped.categoryTags() : before.category_type(),
                        (swapped != null) ? swapped.useTime() : before.operating_hours(),
                        (op.description() != null) ? op.description() : before.description());

                if (slot.course.place_id() != null) changed.add(slot.course.place_id());
                return null;
            }
            default -> {
                return "알 수 없는 op: " + op.op();
            }
        }
    }

    private static Slot slotAt(TreeMap<Integer, List<Slot>> byDay, int day, int position) {
        List<Slot> slots = byDay.get(day);
        if (slots == null || position < 1 || position > slots.size()) return null;
        return slots.get(position - 1);
    }

    private static List<GeneratedCourseDto> rebuild(TreeMap<Integer, List<Slot>> byDay,
                                                    TreeMap<Integer, List<GeneratedCourseDto>> appended) {
        TreeMap<Integer, List<GeneratedCourseDto>> merged = new TreeMap<>();

        byDay.forEach((day, slots) -> {
            List<GeneratedCourseDto> dayCourses = merged.computeIfAbsent(day, k -> new ArrayList<>());
            for (Slot slot : slots) {
                dayCourses.addAll(slot.insertedBefore);
                if (!slot.removed) dayCourses.add(slot.course);
            }
        });
        appended.forEach((day, extra) ->
                merged.computeIfAbsent(day, k -> new ArrayList<>()).addAll(extra));

        List<GeneratedCourseDto> result = new ArrayList<>();
        merged.forEach((day, dayCourses) -> {
            dayCourses.sort(Comparator.comparing(GeneratedCourseDto::start_time));
            result.addAll(dayCourses);
        });
        return result;
    }
}
