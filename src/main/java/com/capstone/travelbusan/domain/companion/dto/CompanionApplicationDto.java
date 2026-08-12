package com.capstone.travelbusan.domain.companion.dto;

import com.capstone.travelbusan.domain.companion.entity.CompanionApplication;
import com.capstone.travelbusan.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public class CompanionApplicationDto {

    // ===== 참여자: 신청 요청 =====
    @Getter
    public static class CreateRequest {
        private String introduction;
    }

    // ===== 응답 =====
    @Getter
    @Builder
    public static class Response {
        private UUID applicationId;
        private UUID companionId;
        private String introduction;
        private String status;
        private boolean noShow;
        private LocalDateTime createdAt;

        // 신청자 프로필 — 승인 전에는 배지 수준만 노출 (컨트롤러/프론트에서 마스킹 정책 적용)
        private String applicantId;
        private String applicantNickname;
        private String applicantProfileImageUrl;
        private boolean applicantPhoneVerified;
        private Integer applicantBirthYear;
        private String applicantGender;

        public static Response from(CompanionApplication application) {
            User applicant = application.getApplicant();
            return Response.builder()
                    .applicationId(application.getApplicationId())
                    .companionId(application.getCompanion().getCompanionId())
                    .introduction(application.getIntroduction())
                    .status(application.getStatus())
                    .noShow(application.isNoShow())
                    .createdAt(application.getCreatedAt())
                    .applicantId(applicant.getId().toString())
                    .applicantNickname(applicant.getNickname())
                    .applicantProfileImageUrl(applicant.getProfileImageUrl())
                    .applicantPhoneVerified(applicant.isPhoneVerified())
                    .applicantBirthYear(applicant.getBirthYear())
                    .applicantGender(applicant.getGender())
                    .build();
        }
    }
}
