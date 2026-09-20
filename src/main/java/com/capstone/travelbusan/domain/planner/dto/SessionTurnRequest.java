package com.capstone.travelbusan.domain.planner.dto;

import java.util.List;

/**
 * 대화 한 턴.
 *
 * @param text       사용자 발화
 * @param categories 관심 카테고리 (새 일정 생성 턴에서만 의미가 있다)
 * @param lang       이 턴의 언어. 생략하면 세션에 저장된 값을 쓴다
 */
public record SessionTurnRequest(String text, List<String> categories, String lang) {}
