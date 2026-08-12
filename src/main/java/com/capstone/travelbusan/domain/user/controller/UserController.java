package com.capstone.travelbusan.domain.user.controller;

import com.capstone.travelbusan.domain.companion.dto.CompanionReviewDto;
import com.capstone.travelbusan.domain.companion.service.CompanionReviewService;
import com.capstone.travelbusan.domain.user.dto.UserDto;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final CompanionReviewService companionReviewService;

    // 내 프로필 조회 (본인 인증 상태·나이대·성별·동행 이력 카운트 포함)
    // 기존 로그인 응답(AuthDto.AuthData)에는 이 정보가 없어 마이페이지/동행 진입 시 별도 조회가 필요하다.
    @GetMapping("/me")
    public ResponseEntity<UserDto.MeResponse> getMe(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return ResponseEntity.ok(new UserDto.MeResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.isGuide(),
                user.isPhoneVerified(),
                user.getBirthYear(),
                user.getGender(),
                user.getCompanionHostCount(),
                user.getCompanionJoinCount(),
                user.getNoShowCount(),
                user.getSanctionLevel(),
                user.getRestrictedUntil()
        ));
    }

    // 마이페이지: 동행 이력 + 평가 요약 (참여/방장 횟수, 평균 평점 — Phase 6 가이드 전환 심사의 기초 자료)
    @GetMapping("/me/companion-summary")
    public ResponseEntity<CompanionReviewDto.Summary> getMyCompanionSummary(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(companionReviewService.getMySummary(currentUser.getUserId()));
    }
}
