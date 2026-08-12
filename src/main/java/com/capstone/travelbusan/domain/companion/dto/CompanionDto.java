package com.capstone.travelbusan.domain.companion.dto;

import com.capstone.travelbusan.domain.companion.entity.Companion;
import com.capstone.travelbusan.domain.planner.entity.Itinerary;
import com.capstone.travelbusan.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CompanionDto {

    // ===== 방장: 모집글 작성 요청 =====
    @Getter
    public static class CreateRequest {
        private Long itineraryId;
        private String title;
        private Integer minParticipants;
        private Integer maxParticipants;
        private List<String> preferenceTags;
        private String costSharingNote;
        private String description;
        private Integer minAge;
        private Integer maxAge;
        private String snsHandle;
    }

    // ===== 방장: 최소 인원 미달 시 결정 요청 =====
    // decision: "CONTINUE"(소규모 진행) / "POSTPONE"(연기) / "CANCEL"(취소)
    @Getter
    public static class DecisionRequest {
        private String decision;
        private LocalDate newStartDate;
        private LocalDate newEndDate;
    }

    // ===== 참여자: 신청 요청은 CompanionApplicationDto 참고 =====

    // ===== 응답 =====
    @Getter
    @Builder
    public static class Response {
        private UUID companionId;
        private String status;
        private String title;
        private Integer minParticipants;
        private Integer maxParticipants;
        private long approvedCount;
        private List<String> preferenceTags;
        private String costSharingNote;
        private String description;
        private Integer minAge;
        private Integer maxAge;
        private String snsHandle;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime createdAt;

        // 일정(Itinerary) 요약
        private Long itineraryId;
        private String itineraryTitle;
        private String region;

        // 방장 프로필 — Phase 0 신원 공개 정책: 신청/탐색 단계는 인증 배지·나이대·성별까지만 노출
        private String hostId;
        private String hostNickname;
        private String hostProfileImageUrl;
        private boolean hostPhoneVerified;
        private Integer hostBirthYear;
        private String hostGender;

        // 모집글 부스트 (Phase 7)
        private boolean boosted;

        public static Response from(Companion companion, long approvedCount) {
            Itinerary itinerary = companion.getItinerary();
            User host = companion.getHost();

            return Response.builder()
                    .companionId(companion.getCompanionId())
                    .status(companion.getStatus())
                    .title(companion.getTitle())
                    .minParticipants(companion.getMinParticipants())
                    .maxParticipants(companion.getMaxParticipants())
                    .approvedCount(approvedCount)
                    .preferenceTags(companion.getPreferenceTags())
                    .costSharingNote(companion.getCostSharingNote())
                    .description(companion.getDescription())
                    .minAge(companion.getMinAge())
                    .maxAge(companion.getMaxAge())
                    .snsHandle(companion.getSnsHandle())
                    .startDate(companion.getStartDate())
                    .endDate(companion.getEndDate())
                    .createdAt(companion.getCreatedAt())
                    .itineraryId(itinerary.getItineraryId())
                    .itineraryTitle(itinerary.getTitle())
                    .region(itinerary.getRegion())
                    .hostId(host.getId().toString())
                    .hostNickname(host.getNickname())
                    .hostProfileImageUrl(host.getProfileImageUrl())
                    .hostPhoneVerified(host.isPhoneVerified())
                    .hostBirthYear(host.getBirthYear())
                    .hostGender(host.getGender())
                    .boosted(companion.isBoosted())
                    .build();
        }
    }
}
