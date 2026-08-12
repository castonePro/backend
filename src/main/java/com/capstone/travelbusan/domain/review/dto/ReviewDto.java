package com.capstone.travelbusan.domain.review.dto;

import com.capstone.travelbusan.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class ReviewDto {

    @Getter
    public static class Request {
        private UUID guideId;
        private UUID serviceId;
        private BigDecimal rating;
        private String content;
    }

    @Getter
    @Builder
    public static class Response {
        private UUID reviewId;
        private UUID guideId;
        private String guideNickname;
        private UUID userId;
        private String userNickname;
        private BigDecimal rating;
        private String content;
        private LocalDateTime createdAt;

        public static Response from(Review review) {
            return Response.builder()
                    .reviewId(review.getReviewId())
                    .guideId(review.getGuide().getId())
                    .guideNickname(review.getGuide().getNickname())
                    .userId(review.getUser().getId())
                    .userNickname(review.getUser().getNickname())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .createdAt(review.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Summary {
        private UUID guideId;
        private Double averageRating;
        private long reviewCount;
    }
}