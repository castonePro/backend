package com.capstone.travelbusan.domain.report.dto;

import com.capstone.travelbusan.domain.report.entity.Report;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReportDto {

    // ===== 신고 접수 요청 =====
    @Getter
    public static class CreateRequest {
        private UUID reportedUserId;
        private UUID companionId; // 선택 — 특정 동행 문맥에서 신고하는 경우
        private String reasonCategory; // NO_SHOW / HARASSMENT / INAPPROPRIATE_BEHAVIOR / FRAUD / OTHER
        private String description;
    }

    // ===== 응답 =====
    @Getter
    @Builder
    public static class Response {
        private UUID reportId;
        private String reportedUserId;
        private String reportedUserNickname;
        private UUID companionId;
        private String reasonCategory;
        private String description;
        private String status;
        private LocalDateTime createdAt;

        public static Response from(Report report) {
            return Response.builder()
                    .reportId(report.getReportId())
                    .reportedUserId(report.getReportedUser().getId().toString())
                    .reportedUserNickname(report.getReportedUser().getNickname())
                    .companionId(report.getCompanion() != null ? report.getCompanion().getCompanionId() : null)
                    .reasonCategory(report.getReasonCategory())
                    .description(report.getDescription())
                    .status(report.getStatus())
                    .createdAt(report.getCreatedAt())
                    .build();
        }
    }
}
