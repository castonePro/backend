package com.capstone.travelbusan.domain.planner.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// PlannerSaveRequest.java
public record PlannerSaveRequest(
        String status,
        ItinerarySaveDto data
) {}
