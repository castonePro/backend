package com.capstone.travelbusan.domain.planner.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ItineraryResponseDto {
    private Long itineraryId;
    private String title;
    private String region;
    private LocalDate startDate;      // DATE 타입 매핑
    private LocalDate endDate;        // DATE 타입 매핑
    private List<ItineraryDetailResponseDto> details; // 상세 코스 리스트
}