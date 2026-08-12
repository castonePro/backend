package com.capstone.travelbusan.domain.planner.dto;

import java.time.LocalDate;
import java.util.List;

// ItinerarySaveDto.java
public record ItinerarySaveDto(
        String title,
        String region,
        LocalDate start_date,
        LocalDate end_date,
        List<CourseSaveDto> generated_courses
) {}
