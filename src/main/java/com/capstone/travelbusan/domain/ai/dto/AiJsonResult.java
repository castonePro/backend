package com.capstone.travelbusan.domain.ai.dto;

/**
 * 구조화 응답 + 원본 호출 결과(토큰 사용량 포함).
 *
 * <p>멀티턴에서는 턴마다 토큰이 누적되므로, 파싱된 값만 받으면 세션당 비용 상한을
 * 걸 수가 없다. 값과 사용량을 함께 돌려주는 통로가 필요하다.
 */
public record AiJsonResult<T>(T value, AiChatResult raw) {}
