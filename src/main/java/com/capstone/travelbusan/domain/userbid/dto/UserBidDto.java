package com.capstone.travelbusan.domain.userbid.dto;

import com.capstone.travelbusan.domain.userbid.entity.UserBid;
import com.capstone.travelbusan.domain.planner.entity.Itinerary;
import com.capstone.travelbusan.domain.planner.entity.ItineraryDetail;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class UserBidDto {

    // ===== 요청 =====

    @Getter
    public static class Request {
        private Long itineraryId;
    }

    // ===== 응답 =====

    @Getter
    @Builder
    public static class Response {
        private UUID bidId;
        private String status;
        private LocalDateTime createdAt;

        // 일정 정보
        private Long itineraryId;
        private String title;
        private String region;
        private LocalDate startDate;
        private LocalDate endDate;

        // 제안한 사용자 정보
        private String userNickname;

        // 상세 코스
        private List<CourseDto> courses;

        public static Response from(UserBid userBid) {
            Itinerary itinerary = userBid.getItinerary();
            List<CourseDto> courses = itinerary.getDetails().stream()
                    .map(CourseDto::from)
                    .toList();

            return Response.builder()
                    .bidId(userBid.getBidId())
                    .status(userBid.getStatus())
                    .createdAt(userBid.getCreatedAt())
                    .itineraryId(itinerary.getItineraryId())
                    .title(itinerary.getTitle())
                    .region(itinerary.getRegion())
                    .startDate(itinerary.getStartDate())
                    .endDate(itinerary.getEndDate())
                    .userNickname(userBid.getUser().getNickname())
                    .courses(courses)
                    .build();
        }
    }

    // ===== 코스 DTO =====

    @Getter
    @Builder
    public static class CourseDto {
        private Integer dayNumber;
        private LocalTime startTime;
        private Integer durationMinutes;
        private String placeName;
        private List<String> categoryType;
        private String description;
        private Integer sortOrder;

        public static CourseDto from(ItineraryDetail detail) {
            return CourseDto.builder()
                    .dayNumber(detail.getDayNumber())
                    .startTime(detail.getStartTime())
                    .durationMinutes(detail.getDurationMinutes())
                    .placeName(detail.getPlaceName())
                    .categoryType(detail.getCategoryType())
                    .description(detail.getDescription())
                    .sortOrder(detail.getSortOrder())
                    .build();
        }
    }
}