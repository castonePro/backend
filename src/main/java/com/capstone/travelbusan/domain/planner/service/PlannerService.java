package com.capstone.travelbusan.domain.planner.service;

import com.capstone.travelbusan.domain.ai.dto.AiChatOptions;
import com.capstone.travelbusan.domain.ai.dto.AiJsonResult;
import com.capstone.travelbusan.domain.ai.dto.AiMessage;
import com.capstone.travelbusan.domain.ai.service.AiService;
import com.capstone.travelbusan.domain.planner.dto.*;
import com.capstone.travelbusan.domain.planner.entity.Itinerary;
import com.capstone.travelbusan.domain.planner.entity.ItineraryDetail;
import com.capstone.travelbusan.domain.planner.repository.ItineraryDetailRepository;
import com.capstone.travelbusan.domain.planner.repository.ItineraryRepository;
import com.capstone.travelbusan.domain.planner.support.PlaceCandidate;
import com.capstone.travelbusan.domain.planner.support.PlanValidator;
import com.capstone.travelbusan.domain.planner.support.PlannerPromptFactory;
import com.capstone.travelbusan.domain.recommend_place.repository.TravelPlaceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlannerService {

    /** 벡터 검색에서 읽어올 행 수. 한 장소가 여러 chunk를 가질 수 있어 넉넉히 읽고 중복을 제거한다. */
    private static final int VECTOR_FETCH_ROWS = 90;
    /** LLM에 실제로 넘길 서로 다른 장소 수. */
    private static final int MAX_CANDIDATES = 30;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AiService aiService;
    private final JdbcTemplate jdbcTemplate;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryDetailRepository detailRepository;
    private final TravelPlaceRepository travelPlaceRepository;

    // ───────────────────────── 일정 생성 ─────────────────────────

    /**
     * RAG + LLM으로 일정을 생성하고, 서버에서 검증·보강해 반환한다.
     *
     * <p>이전 구현과 달라진 점:
     * <ul>
     *   <li>LLM 응답을 Structured Outputs로 강제해 ```json 래퍼 파싱이 필요 없다</li>
     *   <li>LLM은 place_id만 고르고, 장소명·좌표·운영시간은 DB 값으로 채운다</li>
     *   <li>후보 목록에 없는 place_id는 서버가 걸러내므로 환각 장소가 통과할 수 없다</li>
     *   <li>lang을 받아 결과 언어를 지정한다</li>
     * </ul>
     */
    public PlanGeneration generateTravelPlan(PlannerRequest request) {
        String searchText = buildSearchText(request);

        log.info("[1/4] 후보 검색 시작: text={}", searchText);
        List<PlaceCandidate> candidates = findCandidates(searchText, MAX_CANDIDATES);
        log.info("[1/4] 검색 완료: 서로 다른 장소 {}개", candidates.size());
        if (candidates.isEmpty()) {
            throw new IllegalStateException("조건에 맞는 장소를 찾지 못했습니다.");
        }

        String defaultStartDate = LocalDate.now(SEOUL).plusDays(1).format(DATE_FMT);
        String language = PlannerPromptFactory.languageName(request.lang());

        List<AiMessage> messages = List.of(
                PlannerPromptFactory.systemMessage(language, defaultStartDate),
                PlannerPromptFactory.candidatesMessage(candidates),
                PlannerPromptFactory.requestMessage(request.prompt(), request.categories()));

        AiChatOptions options = AiChatOptions.defaults()
                .withTemperature(0.4)
                .withMaxTokens(4000)
                .withResponseSchema(PlannerPromptFactory.planSchema());

        log.info("[2/4] LLM 일정 생성 시작: lang={}, language={}", request.lang(), language);
        AiJsonResult<LlmPlanDraft> result =
                aiService.chatAsJsonWithUsage(messages, options, LlmPlanDraft.class);
        LlmPlanDraft draft = result.value();

        log.info("[3/4] 검증 및 DB 값 보강 시작: 코스 {}건",
                draft.generated_courses() == null ? 0 : draft.generated_courses().size());
        GeneratedPlanDto plan = PlanValidator.validateAndEnrich(draft, candidates, defaultStartDate);
        log.info("[4/4] 완료: 유효 코스 {}건", plan.generated_courses().size());

        return new PlanGeneration(
                plan,
                (draft.reply() != null && !draft.reply().isBlank()) ? draft.reply() : plan.title(),
                result.raw().promptTokens(),
                result.raw().completionTokens());
    }

    /**
     * 임베딩 + 벡터 검색으로 장소 후보를 뽑는다.
     * 멀티턴의 수정(patch) 경로에서도 "새로 넣을 수 있는 장소"의 화이트리스트로 쓴다.
     */
    public List<PlaceCandidate> findCandidates(String searchText, int limit) {
        String text = (searchText == null || searchText.isBlank()) ? "부산 여행" : searchText;
        String vectorString = aiService.getEmbedding(text);
        return searchCandidates(vectorString, limit);
    }

    private String buildSearchText(PlannerRequest request) {
        String categories = (request.categories() != null && !request.categories().isEmpty())
                ? String.join(", ", request.categories())
                : "";
        String prompt = request.prompt() != null ? request.prompt() : "";
        String combined = (categories + " " + prompt).trim();
        return combined.isBlank() ? "부산 여행" : combined;
    }

    /**
     * Oracle 23ai AI Vector Search로 후보를 뽑는다.
     *
     * <p>travel_vectors는 장소 하나에 여러 chunk가 붙을 수 있어서, 단순히 30행을 읽으면
     * 서로 다른 장소가 30개보다 훨씬 적게 나올 수 있다. 넉넉히 읽고 place_id 기준으로
     * 첫 등장(=가장 유사한 chunk)만 남긴다.
     */
    private List<PlaceCandidate> searchCandidates(String vectorString, int limit) {
        String sql = """
                SELECT p.place_id, p.title, p.addr1, p.cat1, p.cat2, p.cat3, f.use_time, v.content_chunk,
                       p.location.SDO_POINT.X AS mapx,
                       p.location.SDO_POINT.Y AS mapy
                FROM travel_places p
                JOIN travel_vectors v ON p.place_id = v.place_id
                LEFT JOIN travel_fees f ON p.place_id = f.place_id
                ORDER BY VECTOR_DISTANCE(v.embedding, TO_VECTOR(?), COSINE) ASC
                FETCH FIRST %d ROWS ONLY
                """.formatted(VECTOR_FETCH_ROWS);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, vectorString);

        Map<Long, PlaceCandidate> distinct = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long placeId = asLong(row, "place_id");
            if (placeId == null || distinct.containsKey(placeId)) continue;

            distinct.put(placeId, new PlaceCandidate(
                    placeId,
                    asString(row, "title"),
                    asString(row, "addr1"),
                    asString(row, "cat1"),
                    asString(row, "cat2"),
                    asString(row, "cat3"),
                    asString(row, "use_time"),
                    asDouble(row, "mapy"),   // SDO_POINT.Y = 위도
                    asDouble(row, "mapx"),   // SDO_POINT.X = 경도
                    asString(row, "content_chunk")));

            if (distinct.size() >= limit) break;
        }
        return List.copyOf(distinct.values());
    }

    // ───────────────────────── 일정 저장/조회 ─────────────────────────

    @Transactional
    public Long saveItinerary(ItinerarySaveDto dto, UUID userId) {
        Itinerary itinerary = Itinerary.builder()
                .userId(userId)
                .title(dto.title())
                .region(dto.region())
                .startDate(dto.start_date())
                .endDate(dto.end_date())
                .build();

        List<ItineraryDetail> details = new ArrayList<>();
        List<CourseSaveDto> courses = dto.generated_courses();

        for (int i = 0; i < courses.size(); i++) {
            CourseSaveDto course = courses.get(i);

            ItineraryDetail detail = ItineraryDetail.builder()
                    .itinerary(itinerary)
                    .dayNumber(course.day_number())
                    .startTime(course.start_time())
                    .durationMinutes(course.duration_minutes())
                    .placeName(course.place())
                    .categoryType(course.category_type())
                    .operatingHours(course.operating_hours())
                    .description(course.description())
                    .latitude(course.latitude())
                    .longitude(course.longitude())
                    .placeId(resolvePlaceId(course))
                    .sortOrder(i + 1)
                    .build();

            details.add(detail);
        }

        itinerary.setDetails(details);
        return itineraryRepository.save(itinerary).getItineraryId();
    }

    /**
     * 생성 응답에 place_id가 들어 있으면 그대로 쓴다.
     * 구버전 클라이언트가 place_id 없이 보내는 경우에만 이름으로 조회한다.
     */
    private Long resolvePlaceId(CourseSaveDto course) {
        if (course.place_id() != null) {
            return course.place_id();
        }
        if (course.place() == null || course.place().isBlank()) {
            return null;
        }
        return travelPlaceRepository.findFirstByTitle(course.place())
                .map(p -> p.getPlaceId().longValue())
                .orElseGet(() -> {
                    log.warn("place_id를 찾지 못했습니다: place='{}'", course.place());
                    return null;
                });
    }

    @Transactional
    public ItineraryResponseDto getMyItinerary(Long itineraryId, UUID currentUserId) {
        Itinerary itinerary = itineraryRepository.findByItineraryIdAndUserId(itineraryId, currentUserId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없거나 접근 권한이 없습니다."));

        List<ItineraryDetail> details =
                detailRepository.findByItinerary_ItineraryIdOrderByDayNumberAscSortOrderAsc(itineraryId);

        return ItineraryResponseDto.builder()
                .itineraryId(itinerary.getItineraryId())
                .title(itinerary.getTitle())
                .region(itinerary.getRegion())
                .startDate(itinerary.getStartDate())
                .endDate(itinerary.getEndDate())
                .details(details.stream().map(this::convertToDetailDto).toList())
                .build();
    }

    @Transactional
    public List<ItineraryResponseDto> getAllMyItineraries(UUID currentUserId) {
        return itineraryRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserId).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteItinerary(Long itineraryId, UUID userId) {
        Itinerary itinerary = itineraryRepository.findByItineraryIdAndUserId(itineraryId, userId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없거나 접근 권한이 없습니다."));
        itineraryRepository.delete(itinerary);
    }

    private ItineraryDetailResponseDto convertToDetailDto(ItineraryDetail detail) {
        return ItineraryDetailResponseDto.builder()
                .detailId(detail.getDetailId())
                .dayNumber(detail.getDayNumber())
                .startTime(detail.getStartTime())
                .durationMinutes(detail.getDurationMinutes())
                .placeName(detail.getPlaceName())
                .categoryType(detail.getCategoryType())
                .operatingHours(detail.getOperatingHours())
                .description(detail.getDescription())
                .placeId(detail.getPlaceId())
                .sortOrder(detail.getSortOrder())
                .latitude(detail.getLatitude())
                .longitude(detail.getLongitude())
                .build();
    }

    private ItineraryResponseDto convertToResponseDto(Itinerary itinerary) {
        return ItineraryResponseDto.builder()
                .itineraryId(itinerary.getItineraryId())
                .title(itinerary.getTitle())
                .region(itinerary.getRegion())
                .startDate(itinerary.getStartDate())
                .endDate(itinerary.getEndDate())
                .details(itinerary.getDetails().stream()
                        .map(this::convertToDetailDto)
                        .toList())
                .build();
    }

    // ───────────────────────── 값 정규화 헬퍼 ─────────────────────────

    /** Oracle은 컬럼명을 대문자로 돌려주는 경우가 있어 대소문자 모두 확인한다. */
    private static Object col(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) v = row.get(key.toUpperCase());
        if (v == null) v = row.get(key.toLowerCase());
        return v;
    }

    private static String asString(Map<String, Object> row, String key) {
        Object v = col(row, key);
        return (v != null) ? String.valueOf(v) : null;
    }

    private static Long asLong(Map<String, Object> row, String key) {
        Object v = col(row, key);
        if (v instanceof Number n) return n.longValue();
        if (v == null) return null;
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double asDouble(Map<String, Object> row, String key) {
        Object v = col(row, key);
        if (v instanceof Number n) return n.doubleValue();
        if (v == null) return null;
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
