package com.capstone.travelbusan.domain.guide.controller;

import com.capstone.travelbusan.domain.guide.dto.request.GuideProductRequestDto;
import com.capstone.travelbusan.domain.guide.dto.response.GuideProductResponseDto;
import com.capstone.travelbusan.domain.guide.service.GuideProductService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/guide")
@RequiredArgsConstructor
public class GuideProductController {

    private final GuideProductService guideProductService;

    // ==================== 사용자용 ====================

    // 게시된 상품 전체 목록 (사용자 가이드 탐색 화면)
    @GetMapping("/products")
    public ResponseEntity<List<GuideProductResponseDto>> getAllPublishedProducts() {
        return ResponseEntity.ok(guideProductService.getAllPublishedProducts());
    }

    // ==================== 가이드용 ====================

    // 내 상품 전체 목록 (미게시 포함, 상품 관리 화면)
    @GetMapping("/my-products")
    public ResponseEntity<List<GuideProductResponseDto>> getMyProducts(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(guideProductService.getMyProducts(currentUser.getUserId()));
    }

    // 내 게시된 상품 목록 (포트폴리오 화면)
    @GetMapping("/my-products/published")
    public ResponseEntity<List<GuideProductResponseDto>> getMyPublishedProducts(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(guideProductService.getMyPublishedProducts(currentUser.getUserId()));
    }

    // 상품 등록
    @PostMapping("/products")
    public ResponseEntity<GuideProductResponseDto> createProduct(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody GuideProductRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(guideProductService.createProduct(currentUser.getUserId(), request));
    }

    // 상품 수정
    @PutMapping("/products/{serviceId}")
    public ResponseEntity<GuideProductResponseDto> updateProduct(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID serviceId,
            @RequestBody GuideProductRequestDto request) {
        return ResponseEntity.ok(
                guideProductService.updateProduct(currentUser.getUserId(), serviceId, request));
    }

    // 상품 삭제
    @DeleteMapping("/products/{serviceId}")
    public ResponseEntity<Void> deleteProduct(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID serviceId) {
        guideProductService.deleteProduct(currentUser.getUserId(), serviceId);
        return ResponseEntity.noContent().build();
    }

    // 게시 / 게시취소 토글
    @PatchMapping("/products/{serviceId}/publish")
    public ResponseEntity<GuideProductResponseDto> togglePublish(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID serviceId) {
        return ResponseEntity.ok(
                guideProductService.togglePublish(currentUser.getUserId(), serviceId));
    }
}