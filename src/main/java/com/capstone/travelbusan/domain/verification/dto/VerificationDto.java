package com.capstone.travelbusan.domain.verification.dto;

public class VerificationDto {

    // 인증번호 발송 요청
    public record SendCodeRequest(
            String phoneNumber
    ) {}

    // 인증번호 발송 응답
    // devCode는 Mock 공급자에서만 채워지며, 실제 PG(Firebase/PortOne) 연동 시 제거된다.
    public record SendCodeResponse(
            String status,
            String verificationId,
            String devCode
    ) {}

    // 인증번호 확인 요청 (본인 인증 완료 시 프로필에 반영될 정보 포함)
    public record ConfirmRequest(
            String verificationId,
            String code,
            String phoneNumber,
            Integer birthYear,
            String gender
    ) {}

    // 인증번호 확인 응답
    public record ConfirmResponse(
            String status,
            boolean phoneVerified
    ) {}
}
