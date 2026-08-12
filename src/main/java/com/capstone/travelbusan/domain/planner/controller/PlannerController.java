package com.capstone.travelbusan.domain.planner.controller;

import com.capstone.travelbusan.domain.planner.dto.ItineraryResponseDto;
import com.capstone.travelbusan.domain.planner.dto.PlannerRequest;
import com.capstone.travelbusan.domain.planner.dto.PlannerResponse;
import com.capstone.travelbusan.domain.planner.dto.PlannerSaveRequest;
import com.capstone.travelbusan.domain.planner.service.PlannerService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;
    private final ObjectMapper objectMapper;

    @PostMapping("/generate")
    public ResponseEntity<PlannerResponse> generate(@RequestBody PlannerRequest request) {
        try {
            String gptJsonResponse = plannerService.generateTravelPlan(request);
            Object data = objectMapper.readValue(gptJsonResponse, Object.class);
            return ResponseEntity.ok(new PlannerResponse("success", data));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new PlannerResponse("error", "일정 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<Long> savePlanner(
            @RequestBody PlannerSaveRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
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