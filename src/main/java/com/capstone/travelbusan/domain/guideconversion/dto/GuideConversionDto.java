package com.capstone.travelbusan.domain.guideconversion.dto;

import com.capstone.travelbusan.domain.guideconversion.entity.GuideApplication;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class GuideConversionDto {

    // ===== 전환 신청 요청 =====
    @Getter
    public static class ApplyRequest {
        private String message; // 선택 — 신청 사유·각오 등
    }

    // ===== 운영자 심사 요청 =====
    @Getter
    public static class ReviewRequest {
        private String decision; // APPROVE / REJECT
        private String note;
    }

    // ===== 신청 이력 응답 =====
    @Getter
    @Builder
    public static class ApplicationResponse {
        private UUID applicationId;
        private String message;
        private String status;
        private LocalDateTime appliedAt;
        private LocalDateTime reviewedAt;
        private String reviewNote;

        public static ApplicationResponse from(GuideApplication application) {
            return ApplicationResponse.builder()
                    .applicationId(application.getApplicationId())
                    .message(application.getMessage())
                    .status(application.getStatus())
                    .appliedAt(application.getAppliedAt())
                    .reviewedAt(application.getReviewedAt())
                    .reviewNote(application.getReviewNote())
                    .build();
        }
    }

    // ===== 전환 진행 현황 =====
    @Getter
    @Builder
    public static class StatusResponse {
        private int companionHostCount;
        private int companionJoinCount;
        private BigDecimal operationAverageRating;

        private boolean meetsHostRequirement;   // 방장 3회 이상
        private boolean meetsJoinRequirement;    // 참여 3회 이상
        private boolean meetsRatingRequirement;  // 운영 평가 4.0 이상

        private boolean eligible;          // 세 조건 모두 충족 = 예비 가이드
        private boolean preliminaryGuide;  // 서버에 예비 가이드 배지가 실제로 기록되어 있는지
        private boolean alreadyGuide;      // 이미 가이드로 등록된 사용자인지 (is_guide)

        private ApplicationResponse latestApplication; // null 가능
    }
}
