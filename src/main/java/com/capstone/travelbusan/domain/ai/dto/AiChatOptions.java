package com.capstone.travelbusan.domain.ai.dto;

/**
 * Chat Completions 호출 옵션.
 *
 * @param model          모델명 (null이면 {@link #DEFAULT_MODEL})
 * @param temperature    null이면 API 기본값
 * @param maxTokens      출력 토큰 상한. 비용 방어선이므로 되도록 명시한다
 * @param responseSchema null이 아니면 Structured Outputs(strict)로 강제
 */
public record AiChatOptions(
        String model,
        Double temperature,
        Integer maxTokens,
        AiJsonSchema responseSchema
) {
    /** Structured Outputs를 지원하는 최저가 모델. */
    public static final String DEFAULT_MODEL = "gpt-4o-mini";

    public static AiChatOptions defaults() {
        return new AiChatOptions(DEFAULT_MODEL, null, null, null);
    }

    public AiChatOptions withModel(String model) {
        return new AiChatOptions(model, temperature, maxTokens, responseSchema);
    }

    public AiChatOptions withTemperature(double temperature) {
        return new AiChatOptions(model, temperature, maxTokens, responseSchema);
    }

    public AiChatOptions withMaxTokens(int maxTokens) {
        return new AiChatOptions(model, temperature, maxTokens, responseSchema);
    }

    public AiChatOptions withResponseSchema(AiJsonSchema responseSchema) {
        return new AiChatOptions(model, temperature, maxTokens, responseSchema);
    }
}
