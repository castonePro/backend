package com.capstone.travelbusan.domain.ai.dto;

/**
 * OpenAI Chat Completions의 message 한 건.
 *
 * <p>멀티턴 대화를 만들려면 호출부가 role을 직접 조립할 수 있어야 한다.
 * 기존 {@code getChatResponse(String)}은 user 메시지 하나만 보낼 수 있어서
 * system 프롬프트 분리도, 이전 턴 히스토리 전달도 불가능했다.
 *
 * <p>사용자 입력은 반드시 {@link #user(String)}로만 넣는다.
 * 사용자 텍스트를 system 프롬프트에 String.format으로 끼워넣으면
 * 프롬프트 인젝션에 그대로 노출된다.
 */
public record AiMessage(String role, String content) {

    public static AiMessage system(String content) {
        return new AiMessage("system", content);
    }

    public static AiMessage user(String content) {
        return new AiMessage("user", content);
    }

    public static AiMessage assistant(String content) {
        return new AiMessage("assistant", content);
    }
}
