package com.capstone.travelbusan.domain.planner.dto;

/** 히스토리 복구용 대화 한 줄. */
public record SessionMessageDto(
        int seq,
        String role,
        String content,
        String intent,
        String createdAt
) {}
