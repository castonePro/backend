package com.capstone.travelbusan.domain.verification.service;

import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import com.capstone.travelbusan.domain.verification.dto.VerificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final PhoneVerificationProvider verificationProvider;
    private final UserRepository userRepository;

    // 1. 인증번호 발송
    public VerificationDto.SendCodeResponse sendCode(VerificationDto.SendCodeRequest request) {
        if (request.phoneNumber() == null || request.phoneNumber().isBlank()) {
            throw new IllegalArgumentException("휴대폰 번호를 입력해주세요.");
        }

        PhoneVerificationProvider.VerificationIssue issue = verificationProvider.sendCode(request.phoneNumber());

        return new VerificationDto.SendCodeResponse("success", issue.verificationId(), issue.devCode());
    }

    // 2. 인증번호 확인 + 프로필(생년/성별) 반영 + 인증 완료 처리
    @Transactional
    public VerificationDto.ConfirmResponse confirmCode(UUID userId, VerificationDto.ConfirmRequest request) {
        boolean valid = verificationProvider.confirmCode(request.verificationId(), request.code());
        if (!valid) {
            throw new IllegalArgumentException("인증번호가 올바르지 않거나 만료되었습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.verifyPhone(request.phoneNumber(), request.birthYear(), request.gender());

        return new VerificationDto.ConfirmResponse("success", true);
    }
}
