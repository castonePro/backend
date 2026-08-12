package com.capstone.travelbusan.domain.bidapplication.dto;

import com.capstone.travelbusan.domain.bidapplication.entity.BidApplication;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public class BidApplicationDto {

    @Getter
    public static class Request {
        private UUID bidId;
    }

    @Getter
    @Builder
    public static class Response {
        private UUID applicationId;
        private UUID bidId;
        private UUID guideId;
        private String guideNickname;
        private String guideIntroduction;
        private LocalDateTime createdAt;

        public static Response from(BidApplication app) {
            return Response.builder()
                    .applicationId(app.getApplicationId())
                    .bidId(app.getUserBid().getBidId())
                    .guideId(app.getGuide().getId())
                    .guideNickname(app.getGuide().getNickname())
                    .createdAt(app.getCreatedAt())
                    .build();
        }
    }
}