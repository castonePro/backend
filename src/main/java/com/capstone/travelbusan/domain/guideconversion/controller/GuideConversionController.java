package com.capstone.travelbusan.domain.guideconversion.controller;

import com.capstone.travelbusan.domain.guideconversion.dto.GuideConversionDto;
import com.capstone.travelbusan.domain.guideconversion.service.GuideConversionService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/guide-conversion")
@RequiredArgsConstructor
public class GuideConversionController {

    private final GuideConversionService guideConversionService;

    // 전환 진행 현황 (조건 충족 여부·예비 가이드 배지·최근 신청 상태)
    @GetMapping("/status")
    public ResponseEntity<GuideConversionDto.StatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(guideConversionService.getStatus(currentUser.getUserId()));
    }

    // 정식 가이드 전환 신청
    @PostMapping("/apply")
    public ResponseEntity<GuideConversionDto.ApplicationResponse> apply(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody GuideConversionDto.ApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(guideConversionService.apply(currentUser.getUserId(), request));
    }

    // 내 신청 이력
    @GetMapping("/applications/my")
    public ResponseEntity<List<GuideConversionDto.ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(guideConversionService.getMyApplications(currentUser.getUserId()));
    }

    // 운영자 심사 처리 — 관리자 role 체계가 아직 없어 임시로 인증된 사용자면 호출 가능 (테스트/데모용)
    @PatchMapping("/applications/{applicationId}/review")
    public ResponseEntity<GuideConversionDto.ApplicationResponse> review(
            @PathVariable UUID applicationId,
            @RequestBody GuideConversionDto.ReviewRequest request) {
        return ResponseEntity.ok(guideConversionService.review(applicationId, request));
    }
}
