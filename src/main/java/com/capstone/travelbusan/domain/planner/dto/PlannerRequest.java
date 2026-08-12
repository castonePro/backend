package com.capstone.travelbusan.domain.planner.dto;

import java.util.List;

// PlannerRequest.java
public record PlannerRequest(String prompt, List<String> categories) {}

