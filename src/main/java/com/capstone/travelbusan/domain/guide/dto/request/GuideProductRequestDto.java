package com.capstone.travelbusan.domain.guide.dto.request;

import com.capstone.travelbusan.domain.guide.dto.GuideScheduleDto;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class GuideProductRequestDto {
    private String title;
    private String description;
    private String region;
    private Integer durationMinutes;
    private Integer maxCapacity;
    private BigDecimal pricePerPerson;
    private Boolean hasCar;
    private List<String> availableLanguages;
    private String meetingPoint;
    private String meetingPointDesc;
    private List<String> includedItems;
    private List<String> excludedItems;
    private List<String> relatedMaterials;
    private List<GuideScheduleDto> schedules;
}
