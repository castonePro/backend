package com.capstone.travelbusan.domain.userbid.controller;

import com.capstone.travelbusan.domain.userbid.dto.UserBidDto;
import com.capstone.travelbusan.domain.userbid.service.UserBidService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-bids")
@RequiredArgsConstructor
public class UserBidController {

    private final UserBidService userBidService;

    // 사용자: 역으로 제안하기
    @PostMapping
    public ResponseEntity<UserBidDto.Response> createUserBid(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody UserBidDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userBidService.createUserBid(currentUser.getUserId(), request.getItineraryId()));
    }

    // 가이드: 입찰 현황 전체 조회
    @GetMapping("/guide")
    public ResponseEntity<List<UserBidDto.Response>> getAllUserBids() {
        return ResponseEntity.ok(userBidService.getAllUserBids());
    }

    // 사용자: 본인 요청 목록 조회
    @GetMapping("/my")
    public ResponseEntity<List<UserBidDto.Response>> getMyUserBids(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(userBidService.getMyUserBids(currentUser.getUserId()));
    }
}