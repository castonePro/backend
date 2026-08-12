package com.capstone.travelbusan.domain.guide.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GuideScheduleDto {
    private String startTime;
    private String endTime;
    private String title;
    private String description;
}
