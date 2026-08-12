package com.capstone.travelbusan.domain.bidapplication.controller;

import com.capstone.travelbusan.domain.bidapplication.dto.BidApplicationDto;
import com.capstone.travelbusan.domain.bidapplication.service.BidApplicationService;
import com.capstone.travelbusan.domain.chat.dto.ChatDto;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bid-applications")
@RequiredArgsConstructor
public class BidApplicationController {

    private final BidApplicationService bidApplicationService;

    // 가이드 입찰 참여
    @PostMapping
    public ResponseEntity<BidApplicationDto.Response> apply(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody BidApplicationDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bidApplicationService.apply(currentUser.getUserId(), request.getBidId()));
    }

    // 가이드 참여 취소
    @DeleteMapping("/{bidId}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID bidId) {
        bidApplicationService.cancel(currentUser.getUserId(), bidId);
        return ResponseEntity.noContent().build();
    }

    // 특정 입찰 참여 가이드 목록 (사용자용)
    @GetMapping("/{bidId}")
    public ResponseEntity<List<BidApplicationDto.Response>> getApplications(
            @PathVariable UUID bidId) {
        return ResponseEntity.ok(bidApplicationService.getApplications(bidId));
    }

    // 가이드 선택 → 채팅방 생성
    @PostMapping("/{applicationId}/select")
    public ResponseEntity<ChatDto.RoomResponse> selectGuide(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID applicationId) {
        return ResponseEntity.ok(
                bidApplicationService.selectGuide(currentUser.getUserId(), applicationId));
    }
}