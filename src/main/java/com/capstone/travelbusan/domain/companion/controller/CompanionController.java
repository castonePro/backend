package com.capstone.travelbusan.domain.companion.controller;

import com.capstone.travelbusan.domain.companion.dto.CompanionApplicationDto;
import com.capstone.travelbusan.domain.companion.dto.CompanionDto;
import com.capstone.travelbusan.domain.companion.service.CompanionService;
import com.capstone.travelbusan.domain.payment.dto.PaymentDto;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companions")
@RequiredArgsConstructor
public class CompanionController {

    private final CompanionService companionService;

    // ==================== 방장 ====================

    // 동행 모집하기
    @PostMapping
    public ResponseEntity<CompanionDto.Response> create(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody CompanionDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companionService.createCompanion(currentUser.getUserId(), request));
    }

    // 내가 만든 방 목록
    @GetMapping("/my/hosting")
    public ResponseEntity<List<CompanionDto.Response>> getMyHosting(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(companionService.getMyHosting(currentUser.getUserId()));
    }

    // 수동 마감 (정원 미달 시 UNDER_MINIMUM, 충족 시 CONFIRMED)
    @PatchMapping("/{companionId}/close")
    public ResponseEntity<CompanionDto.Response> close(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId) {
        return ResponseEntity.ok(companionService.closeForApplications(currentUser.getUserId(), companionId));
    }

    // 최소 인원 미달 시 방장 결정 (CONTINUE / POSTPONE / CANCEL)
    @PatchMapping("/{companionId}/decision")
    public ResponseEntity<CompanionDto.Response> decision(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId,
            @RequestBody CompanionDto.DecisionRequest request) {
        return ResponseEntity.ok(
                companionService.applyUnderMinimumDecision(currentUser.getUserId(), companionId, request));
    }

    // 여행 시작 처리
    @PatchMapping("/{companionId}/start")
    public ResponseEntity<CompanionDto.Response> start(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId) {
        return ResponseEntity.ok(companionService.start(currentUser.getUserId(), companionId));
    }

    // 여행 완료 처리
    @PatchMapping("/{companionId}/complete")
    public ResponseEntity<CompanionDto.Response> complete(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId) {
        return ResponseEntity.ok(companionService.complete(currentUser.getUserId(), companionId));
    }

    // 방장 취소
    @PatchMapping("/{companionId}/cancel")
    public ResponseEntity<CompanionDto.Response> cancel(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId) {
        return ResponseEntity.ok(companionService.cancel(currentUser.getUserId(), companionId));
    }

    // 모집글 부스트 결제 (Mock PG — 즉시 승인, 탐색 목록 상단 노출 3일)
    @PatchMapping("/{companionId}/boost")
    public ResponseEntity<PaymentDto.Response> boost(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId) {
        return ResponseEntity.ok(companionService.boost(currentUser.getUserId(), companionId));
    }

    // 신청자 관리 (방장용)
    @GetMapping("/{companionId}/applications")
    public ResponseEntity<List<CompanionApplicationDto.Response>> getApplications(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId) {
        return ResponseEntity.ok(companionService.getApplications(currentUser.getUserId(), companionId));
    }

    @PatchMapping("/applications/{applicationId}/approve")
    public ResponseEntity<CompanionApplicationDto.Response> approve(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID applicationId) {
        return ResponseEntity.ok(companionService.approveApplication(currentUser.getUserId(), applicationId));
    }

    @PatchMapping("/applications/{applicationId}/reject")
    public ResponseEntity<CompanionApplicationDto.Response> reject(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID applicationId) {
        return ResponseEntity.ok(companionService.rejectApplication(currentUser.getUserId(), applicationId));
    }

    // 노쇼 처리 (방장용) — 취소 없이 실제로 나타나지 않은 참여자를 기록
    @PatchMapping("/applications/{applicationId}/no-show")
    public ResponseEntity<CompanionApplicationDto.Response> markNoShow(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID applicationId) {
        return ResponseEntity.ok(companionService.markNoShow(currentUser.getUserId(), applicationId));
    }

    // ==================== 참여자 ====================

    // 동행 탐색 (모집중 목록)
    @GetMapping
    public ResponseEntity<List<CompanionDto.Response>> explore() {
        return ResponseEntity.ok(companionService.getRecruiting());
    }

    // 모집글 상세
    @GetMapping("/{companionId}")
    public ResponseEntity<CompanionDto.Response> getDetail(@PathVariable UUID companionId) {
        return ResponseEntity.ok(companionService.getDetail(companionId));
    }

    // 참여 신청
    @PostMapping("/{companionId}/applications")
    public ResponseEntity<CompanionApplicationDto.Response> apply(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId,
            @RequestBody CompanionApplicationDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companionService.apply(currentUser.getUserId(), companionId, request));
    }

    // 참여 신청 취소
    @DeleteMapping("/applications/{applicationId}")
    public ResponseEntity<Void> cancelApplication(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID applicationId) {
        companionService.cancelApplication(currentUser.getUserId(), applicationId);
        return ResponseEntity.noContent().build();
    }

    // 내가 참여한(승인된) 방 목록
    @GetMapping("/my/joined")
    public ResponseEntity<List<CompanionDto.Response>> getMyJoined(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(companionService.getMyJoined(currentUser.getUserId()));
    }
}
