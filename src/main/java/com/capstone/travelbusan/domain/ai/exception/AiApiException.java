package com.capstone.travelbusan.domain.ai.exception;

/** OpenAI 호출/파싱 실패. 호출부가 AI 계층 장애를 구분해서 잡을 수 있게 한다. */
public class AiApiException extends RuntimeException {

    public AiApiException(String message) {
        super(message);
    }

    public AiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
