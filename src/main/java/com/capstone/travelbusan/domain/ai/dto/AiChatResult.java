package com.capstone.travelbusan.domain.ai.dto;

/**
 * Chat Completions 응답 + 토큰 사용량.
 *
 * <p>토큰을 돌려주는 이유: 멀티턴이 되면 턴마다 입력 토큰이 누적되어 비용이
 * 선형 증가한다. 세션당 상한을 걸려면 호출 지점에서 사용량을 알아야 한다.
 */
public record AiChatResult(
        String content,
        int promptTokens,
        int completionTokens,
        String finishReason
) {
    public int totalTokens() {
        return promptTokens + completionTokens;
    }

    /** max_tokens에 걸려 잘렸는지. true면 content가 불완전한 JSON일 수 있다. */
    public boolean truncated() {
        return "length".equals(finishReason);
    }
}
