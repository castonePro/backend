package com.capstone.travelbusan.domain.planner.service;

import com.capstone.travelbusan.domain.ai.service.AiService;
import com.capstone.travelbusan.domain.planner.dto.*;
import com.capstone.travelbusan.domain.planner.entity.Itinerary;
import com.capstone.travelbusan.domain.planner.entity.ItineraryDetail;
import com.capstone.travelbusan.domain.planner.repository.ItineraryDetailRepository;
import com.capstone.travelbusan.domain.planner.repository.ItineraryRepository;
import com.capstone.travelbusan.domain.recommend_place.repository.TravelPlaceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlannerService {

    private final AiService aiService;
    private final JdbcTemplate jdbcTemplate;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryDetailRepository detailRepository;
    private final TravelPlaceRepository travelPlaceRepository;

    public String generateTravelPlan(PlannerRequest request) {
        // 1. 임베딩 생성 (OpenAI Embedding API 활용)
        String combinedPrompt = String.join(", ", request.categories()) + " " + request.prompt();
        String vectorString = aiService.getEmbedding(combinedPrompt);

        // 2. 관련 장소 검색 (Oracle 23ai AI Vector Search: VECTOR_DISTANCE & FETCH FIRST)
        String sql = """
            SELECT p.title, p.addr1, p.cat1, p.cat2, p.cat3, f.use_time, v.content_chunk,
                   p.mapx, p.mapy 
            FROM travel_places p 
            JOIN travel_vectors v ON p.place_id = v.place_id 
            LEFT JOIN travel_fees f ON p.place_id = f.place_id
            ORDER BY VECTOR_DISTANCE(v.embedding, TO_VECTOR(?), COSINE) ASC
            FETCH FIRST 30 ROWS ONLY
            """;
        List<Map<String, Object>> places = jdbcTemplate.queryForList(sql, vectorString);

        // 3. 문맥(Context) 조립
        String context = places.stream()
                .map(p -> String.format("- %s (%s) / 분류: %s, %s, %s / 운영시간: %s / 좌표: [위도:%s, 경도:%s] / 상세: %s",
                        p.get("title"), p.get("addr1"), p.get("cat1"), p.get("cat2"), p.get("cat3"),
                        p.getOrDefault("use_time", "정보 없음"),
                        p.getOrDefault("mapy", "0.0"), p.getOrDefault("mapx", "0.0"),
                        p.get("content_chunk")))
                .collect(Collectors.joining("\n"));

        // 4. 기준 날짜 설정 (내일 날짜로 설정)
        String tomorrowDate = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .plusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 5. GPT 프롬프트 작성
        String finalPrompt = String.format("""
            너는 부산 여행 전문 플래너야. 아래 제공된 30개의 [장소 데이터]를 분석해서 사용자의 요청에 맞는 여행 일정을 짜줘.
            
            [필수 준수 사항]
            1. 반드시 아래의 JSON 형식을 지키며, JSON 외의 텍스트는 절대 포함하지 마. 혹시 어떻게 짜야될지 모르겠으면 그냥 아래의 JSON 양식 예시에 빈값 담아서 반환해
            2. 'start_date'는 명시된 요청이 없다면 무조건 '%s'로 설정하고, 'end_date'를 계산해.
            3. 'category_type' 필드는 반드시 문자열 배열 형태인 ["카테고리명1", "카테고리명2"]로 반환해.
            4. 각 코스의 'place'에는 순수 장소명만, 'description'에는 장소명 없이 해당 장소에 대한 활동 설명만 적어.
            5. 각 장소의 'latitude'(위도)와 'longitude'(경도)를 정확히 숫자(Float)로 기입해.
            6. [가장 중요] 제공된 좌표(위도, 경도)를 반드시 분석해서, 길에서 버리는 시간이 없도록 지리적으로 가까운 장소들을 묶어 아주 매끄럽고 현실적인 이동 동선을 구성해. 동선이 이리저리 꼬이면 절대 안 돼.
            
            [장소 데이터 후보 30선]
            %s
            
            [사용자 요청]
            요청 내용: %s
            희망 카테고리: %s
            
            [응답 JSON 형식 예시]
            {
              "title": "여행 제목",
              "region": "부산광역시",
              "start_date": "2026-04-05",
              "end_date": "2026-04-07",
              "generated_courses": [
                {
                  "day_number": 1,
                  "start_time": "09:00",
                  "duration_minutes": 120,
                  "place": "태종대",
                  "latitude": 35.0531,
                  "longitude": 129.0878,
                  "category_type": ["자연", "명소"],
                  "operating_hours": "09:00 - 18:00 (월요일 휴무)",
                  "description": "아름다운 자연경관과 해안을 즐길 수 있는 산책 코스입니다."
                }
              ]
            }
            """, tomorrowDate, context, request.prompt(), request.categories());

        // 6. GPT API 호출
        return aiService.getChatResponse(finalPrompt);
    }

    @Transactional
    public ItineraryResponseDto getMyItinerary(Long itineraryId, UUID currentUserId) {

        // 1. 내 일정인지 확인하며 조회 (남의 ID면 여기서 Optional.empty() 반환됨)
        Itinerary itinerary = itineraryRepository.findByItineraryIdAndUserId(itineraryId, currentUserId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없거나 접근 권한이 없습니다."));

        // 2. 상세 코스 조회 (정렬 순서 보장)
        List<ItineraryDetail> details = detailRepository.findByItinerary_ItineraryIdOrderByDayNumberAscSortOrderAsc(itineraryId);

        // 3. DTO 변환 및 반환
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
        // 1. 해당 유저의 모든 일정 엔티티 조회
        List<Itinerary> itineraries = itineraryRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserId);

        // 2. 엔티티 리스트를 DTO 리스트로 변환하여 반환
        return itineraries.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    private ItineraryDetailResponseDto convertToDetailDto(ItineraryDetail detail) {
        return ItineraryDetailResponseDto.builder()
                .detailId(detail.getDetailId())
                .dayNumber(detail.getDayNumber())
                .startTime(detail.getStartTime())
                .durationMinutes(detail.getDurationMinutes())
                .placeName(detail.getPlaceName())
                .categoryType(detail.getCategoryType()) // TEXT[] -> List<String>
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
                // 상세 코스 리스트를 순회하며 각각 DTO로 변환하여 수집
                .details(itinerary.getDetails().stream()
                        .map(this::convertToDetailDto) // 기존에 만든 상세 컨버터 호출
                        .toList())
                .build();
    }

    @Transactional
    public void deleteItinerary(Long itineraryId, UUID userId) {
        Itinerary itinerary = itineraryRepository.findByItineraryIdAndUserId(itineraryId, userId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없거나 접근 권한이 없습니다."));
        itineraryRepository.delete(itinerary);
    }

    @Transactional
    public Long saveItinerary(ItinerarySaveDto dto, UUID userId) {
        // 1. 부모 엔티티(Itinerary) 생성
        Itinerary itinerary = Itinerary.builder()
                .userId(userId)
                .title(dto.title())
                .region(dto.region())
                .startDate(dto.start_date())
                .endDate(dto.end_date())
                .build();

        // 2. 자식 엔티티(ItineraryDetail) 생성 및 연관 관계 설정
        List<ItineraryDetail> details = new ArrayList<>();
        List<CourseSaveDto> courses = dto.generated_courses();

        for (int i = 0; i < courses.size(); i++) {
            CourseSaveDto course = courses.get(i);

            ItineraryDetail detail = ItineraryDetail.builder()
                    .itinerary(itinerary) // 부모 참조 설정
                    .dayNumber(course.day_number())
                    .startTime(course.start_time())
                    .durationMinutes(course.duration_minutes())
                    .placeName(course.place())
                    .categoryType(course.category_type())
                    .operatingHours(course.operating_hours())
                    .description(course.description())
                    .latitude(course.latitude())
                    .longitude(course.longitude())
                    .placeId(travelPlaceRepository.findByTitle(course.place())  // ★ 추가
                            .map(p -> p.getPlaceId().longValue())
                            .orElse(null))
                    .sortOrder(i + 1) // 리스트 순서대로 정렬 순서 부여
                    .build();

            details.add(detail);
        }

        // 3. 연관 관계 편의 메서드를 통해 부모 객체에 자식 리스트 주입
        itinerary.setDetails(details);

        // 4. 저장 (CascadeType.ALL 설정으로 인해 부모 저장 시 자식도 자동 저장됨)
        Itinerary savedItinerary = itineraryRepository.save(itinerary);

        return savedItinerary.getItineraryId();
    }
}