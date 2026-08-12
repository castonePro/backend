package com.capstone.travelbusan.domain.review.controller;

import com.capstone.travelbusan.domain.review.dto.ReviewDto;
import com.capstone.travelbusan.domain.review.service.ReviewService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성
    @PostMapping
    public ResponseEntity<ReviewDto.Response> createReview(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody ReviewDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(currentUser.getUserId(), request));
    }

    // 가이드 리뷰 목록
    @GetMapping("/guide/{guideId}")
    public ResponseEntity<List<ReviewDto.Response>> getGuideReviews(
            @PathVariable UUID guideId) {
        return ResponseEntity.ok(reviewService.getGuideReviews(guideId));
    }

    // 가이드 평점/리뷰 수 요약
    @GetMapping("/guide/{guideId}/summary")
    public ResponseEntity<ReviewDto.Summary> getGuideSummary(
            @PathVariable UUID guideId) {
        return ResponseEntity.ok(reviewService.getGuideSummary(guideId));
    }
}