package com.capstone.travelbusan.domain.companion.dto;

import com.capstone.travelbusan.domain.companion.entity.CompanionReview;
import com.capstone.travelbusan.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CompanionReviewDto {

    // ===== 리뷰 작성 요청 =====
    @Getter
    public static class CreateRequest {
        private UUID revieweeId;
        private BigDecimal rating;
        private List<String> tags;
        private String comment;
        // reviewee가 해당 동행의 방장일 때만 의미 있음. 그 외에는 서비스에서 무시한다.
        private BigDecimal operationRating;
    }

    // ===== 리뷰 응답 (공개된 리뷰에만 사용) =====
    @Getter
    @Builder
    public static class Response {
        private UUID reviewId;
        private UUID companionId;
        private String companionTitle;
        private String reviewerId;
        private String reviewerNickname;
        private String revieweeId;
        private BigDecimal rating;
        private List<String> tags;
        private BigDecimal operationRating;
        private String comment;
        private LocalDateTime createdAt;

        public static Response from(CompanionReview review) {
            User reviewer = review.getReviewer();
            return Response.builder()
                    .reviewId(review.getReviewId())
                    .companionId(review.getCompanion().getCompanionId())
                    .companionTitle(review.getCompanion().getTitle())
                    .reviewerId(reviewer.getId().toString())
                    .reviewerNickname(reviewer.getNickname())
                    .revieweeId(review.getReviewee().getId().toString())
                    .rating(review.getRating())
                    .tags(review.getTags())
                    .operationRating(review.getOperationRating())
                    .comment(review.getComment())
                    .createdAt(review.getCreatedAt())
                    .build();
        }
    }

    // ===== 완료된 동행에서 내가 평가할 수 있는 대상(다른 참여자) 목록 =====
    @Getter
    @Builder
    public static class ReviewableMember {
        private String userId;
        private String nickname;
        private String profileImageUrl;
        private boolean isHost;
        private boolean alreadyReviewed;
    }

    // ===== 특정 동행에서 내가 받은 리뷰 (공개분만 노출 + 대기중 개수) =====
    @Getter
    @Builder
    public static class ReceivedList {
        private List<Response> reviews;
        private int pendingCount; // 아직 더블 블라인드로 공개되지 않은 리뷰 수
    }

    // ===== 마이페이지 동행 이력 요약 =====
    @Getter
    @Builder
    public static class Summary {
        private int companionHostCount;
        private int companionJoinCount;
        private BigDecimal averageRating;      // 공개된 리뷰 기준
        private BigDecimal operationAverageRating; // 방장으로서 받은 운영 평가 평균
        private int reviewCount;
    }
}
