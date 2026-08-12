package com.capstone.travelbusan.domain.companion.controller;

import com.capstone.travelbusan.domain.companion.dto.CompanionReviewDto;
import com.capstone.travelbusan.domain.companion.service.CompanionReviewService;
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
public class CompanionReviewController {

    private final CompanionReviewService companionReviewService;

    // 리뷰 작성 (완료된 동행의 참여자끼리)
    @PostMapping("/{companionId}/reviews")
    public ResponseEntity<CompanionReviewDto.Response> submitReview(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId,
            @RequestBody CompanionReviewDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companionReviewService.submitReview(currentUser.getUserId(), companionId, request));
    }

    // 내가 평가할 수 있는 대상(다른 참여자) 목록
    @GetMapping("/{companionId}/reviews/reviewable")
    public ResponseEntity<List<CompanionReviewDto.ReviewableMember>> getReviewableMembers(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId) {
        return ResponseEntity.ok(companionReviewService.getReviewableMembers(currentUser.getUserId(), companionId));
    }

    // 이 동행에서 내가 받은 리뷰 (더블 블라인드 공개분 + 대기 개수)
    @GetMapping("/{companionId}/reviews/received")
    public ResponseEntity<CompanionReviewDto.ReceivedList> getReceivedReviews(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId) {
        return ResponseEntity.ok(companionReviewService.getReceivedReviews(currentUser.getUserId(), companionId));
    }
}
