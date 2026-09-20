package com.capstone.travelbusan.domain.planner.dto;

import java.util.List;

/**
 * 일정 생성 요청.
 *
 * @param prompt     사용자 자유 입력
 * @param categories 선택한 관심 카테고리
 * @param lang       결과 언어 로케일(ko, en, ja, zh-CN, vi, id). 프론트가 계속 보내고 있었으나
 *                   이 레코드에 필드가 없어 Jackson이 조용히 버리고 있었다.
 */
public record PlannerRequest(String prompt, List<String> categories, String lang) {}
