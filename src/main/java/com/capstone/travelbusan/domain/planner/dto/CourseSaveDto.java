package com.capstone.travelbusan.domain.planner.dto;

import java.time.LocalTime;
import java.util.List;

// CourseSaveDto.java
public record CourseSaveDto(
        int day_number,
        LocalTime start_time,
        int duration_minutes,
        String place,
        double latitude,
        double longitude,
        List<String> category_type,
        String operating_hours,
        String description
) {}
