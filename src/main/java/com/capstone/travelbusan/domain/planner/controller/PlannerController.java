package com.capstone.travelbusan.domain.planner.controller;

import com.capstone.travelbusan.domain.ai.exception.AiApiException;
import com.capstone.travelbusan.domain.planner.dto.GeneratedPlanDto;
import com.capstone.travelbusan.domain.planner.dto.ItineraryResponseDto;
import com.capstone.travelbusan.domain.planner.dto.PlannerRequest;
import com.capstone.travelbusan.domain.planner.dto.PlannerResponse;
import com.capstone.travelbusan.domain.planner.dto.PlannerSaveRequest;
import com.capstone.travelbusan.domain.planner.service.PlannerService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;

    /**
     * 일정 생성. 응답 JSON 파싱은 Structured Outputs가 보장하므로
     * 예전의 ```json 래퍼 벗기기(cleanJsonString)는 더 이상 필요 없다.
     */
    @PostMapping("/generate")
    public ResponseEntity<PlannerResponse> generate(@RequestBody PlannerRequest request) {
        log.info("일정 생성 요청 수신: prompt={}, categories={}, lang={}",
                request.prompt(), request.categories(), request.lang());
        try {
            GeneratedPlanDto plan = plannerService.generateTravelPlan(request).plan();
            return ResponseEntity.ok(new PlannerResponse("success", plan));
        } catch (IllegalStateException e) {
            // 후보 부족, 검증 후 남은 코스 없음 등 — 사용자가 조건을 바꾸면 해결되는 경우
            log.warn("일정 생성 실패(입력/데이터 문제): {}", e.getMessage());
            return ResponseEntity.unprocessableEntity().body(errorBody(e.getMessage()));
        } catch (AiApiException e) {
            log.error("일정 생성 실패(AI 호출): ", e);
            return ResponseEntity.status(502).body(errorBody("AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요."));
        } catch (Exception e) {
            log.error("일정 생성 중 오류 발생: ", e);
            return ResponseEntity.internalServerError().body(errorBody("일정 생성 중 오류가 발생했습니다."));
        }
    }

    /**
     * 프론트 api client는 에러 본문에서 message 또는 data.message를 찾는다.
     * data에 문자열만 담으면 사용자에게 "HTTP 422"만 보이므로 message 키로 감싼다.
     */
    private static PlannerResponse errorBody(String message) {
        return new PlannerResponse("error", Map.of("message", message));
    }

    @PostMapping("/save")
    public ResponseEntity<?> savePlanner(
            @RequestBody PlannerSaveRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (currentUser == null) {
            log.error("일정 저장 실패: 인증 정보(currentUser)가 null입니다. JWT 토큰이 누락되었거나 유효하지 않습니다.");
            return ResponseEntity.status(401).body("로그인이 필요하거나 인증 토큰이 유효하지 않습니다.");
        }
        if (request == null || request.data() == null) {
            log.error("일정 저장 실패: 요청 본문(request.data)이 비어있습니다. request={}", request);
            return ResponseEntity.badRequest().body("저장할 일정 데이터(data)가 누락되었습니다.");
        }
        log.info("일정 저장 요청 수신: userId={}, title={}", currentUser.getUserId(), request.data().title());
        Long savedId = plannerService.saveItinerary(request.data(), currentUser.getUserId());
        return ResponseEntity.ok(savedId);
    }

    @GetMapping("/itineraries/{itinerary_id}")
    public ResponseEntity<ItineraryResponseDto> getItinerary(
            @PathVariable("itinerary_id") Long itineraryId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ItineraryResponseDto response = plannerService.getMyItinerary(itineraryId, currentUser.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/itineraries")
    public ResponseEntity<List<ItineraryResponseDto>> getAllItineraries(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ItineraryResponseDto> response = plannerService.getAllMyItineraries(currentUser.getUserId());
        return ResponseEntity.ok(response);
    }

    // 일정 삭제
    @DeleteMapping("/itineraries/{itinerary_id}")
    public ResponseEntity<Void> deleteItinerary(
            @PathVariable("itinerary_id") Long itineraryId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        plannerService.deleteItinerary(itineraryId, currentUser.getUserId());
        return ResponseEntity.noContent().build();
    }
}