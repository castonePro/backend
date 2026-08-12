package com.capstone.travelbusan.domain.verification.controller;

import com.capstone.travelbusan.domain.verification.dto.VerificationDto;
import com.capstone.travelbusan.domain.verification.service.PhoneVerificationService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/verification/phone")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    // 인증번호 발송 (동행 방 개설·참여 신청 진입 시 호출)
    @PostMapping("/send")
    public ResponseEntity<VerificationDto.SendCodeResponse> send(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody VerificationDto.SendCodeRequest request) {
        return ResponseEntity.ok(phoneVerificationService.sendCode(request));
    }

    // 인증번호 확인 -> User.phoneVerified = true 반영
    @PostMapping("/confirm")
    public ResponseEntity<VerificationDto.ConfirmResponse> confirm(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody VerificationDto.ConfirmRequest request) {
        return ResponseEntity.ok(
                phoneVerificationService.confirmCode(currentUser.getUserId(), request));
    }
}
