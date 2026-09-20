package com.capstone.travelbusan.domain.planner.dto;

import java.util.List;

/**
 * MODIFY 턴의 LLM 응답.
 *
 * <p>대화(reply)와 상태 변경(operations)을 한 스키마 안에서 분리해 받는다.
 * 이렇게 하면 모델이 "네, 바꿨어요!" 같은 자연어를 앞에 붙여도 JSON 파싱이 깨지지 않는다.
 */
public record LlmPatchResult(String reply, List<LlmPatchOperation> operations) {}
