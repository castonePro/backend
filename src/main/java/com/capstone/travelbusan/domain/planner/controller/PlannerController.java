package com.capstone.travelbusan.domain.planner.controller;

import com.capstone.travelbusan.domain.planner.dto.ItineraryResponseDto;
import com.capstone.travelbusan.domain.planner.dto.PlannerRequest;
import com.capstone.travelbusan.domain.planner.dto.PlannerResponse;
import com.capstone.travelbusan.domain.planner.dto.PlannerSaveRequest;
import com.capstone.travelbusan.domain.planner.service.PlannerService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;
    private final ObjectMapper objectMapper;

    @PostMapping("/generate")
    public ResponseEntity<PlannerResponse> generate(@RequestBody PlannerRequest request) {
        try {
            log.info("일정 생성 요청 수신: prompt={}, categories={}", request.prompt(), request.categories());
            String gptJsonResponse = plannerService.generateTravelPlan(request);
            String cleanedJson = cleanJsonString(gptJsonResponse);
            Object data = objectMapper.readValue(cleanedJson, Object.class);
            return ResponseEntity.ok(new PlannerResponse("success", data));
        } catch (Exception e) {
            log.error("일정 생성 중 오류 발생: ", e);
            return ResponseEntity.internalServerError()
                    .body(new PlannerResponse("error", "일정 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    private String cleanJsonString(String rawJson) {
        if (rawJson == null) return "{}";
        String trimmed = rawJson.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
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