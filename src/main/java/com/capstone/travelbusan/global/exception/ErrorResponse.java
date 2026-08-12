package com.capstone.travelbusan.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

// 실패 시 "status": "error" 필드가 반드시 포함되어야 함
// failed_attempts는 특정 상황에서만 포함되도록 설정
public record ErrorResponse(
        String status,
        String message,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer failed_attempts
) {
    public static ErrorResponse error(String message) {
        return new ErrorResponse("error", message, null);
    }

    public static ErrorResponse loginFail(String message, int attempts) {
        return new ErrorResponse("error", message, attempts);
    }
}