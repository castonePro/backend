package com.capstone.travelbusan.domain.planner.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalTime;
import java.util.List;

@Getter
@Builder
public class ItineraryDetailResponseDto {
    private Long detailId;
    private Integer dayNumber;
    private LocalTime startTime;      // TIME 타입 매핑
    private Integer durationMinutes;
    private String placeName;
    private List<String> categoryType; // TEXT[] 배열 매핑
    private String operatingHours;
    private String description;
    private Long placeId;
    private Integer sortOrder;
    private Double latitude;          // DOUBLE PRECISION 매핑
    private Double longitude;         // DOUBLE PRECISION 매핑
}