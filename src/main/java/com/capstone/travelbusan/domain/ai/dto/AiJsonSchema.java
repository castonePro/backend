package com.capstone.travelbusan.domain.ai.dto;

import java.util.Map;

/**
 * OpenAI Structured Outputs(response_format = json_schema)에 넘길 스키마.
 *
 * <p>strict 모드 제약이 있다:
 * <ul>
 *   <li>모든 object에 {@code "additionalProperties": false}가 있어야 한다</li>
 *   <li>object의 모든 property가 {@code required} 배열에 들어가야 한다
 *       (optional을 표현하려면 타입에 "null"을 함께 준다)</li>
 * </ul>
 * 이 제약을 지키면 모델이 스키마를 벗어난 응답을 물리적으로 낼 수 없어서,
 * "```json 래퍼 벗기기" 같은 방어 코드가 필요 없어진다.
 *
 * @param name   스키마 이름(영문/숫자/언더스코어)
 * @param schema JSON Schema 본문을 Map 형태로
 */
public record AiJsonSchema(String name, Map<String, Object> schema) {}
