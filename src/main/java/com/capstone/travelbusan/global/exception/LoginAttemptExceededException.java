package com.capstone.travelbusan.global.exception;

import lombok.Getter;

@Getter
public class LoginAttemptExceededException extends RuntimeException {
    private final int failedAttempts;

    public LoginAttemptExceededException(int failedAttempts) {
        // 명세서에 명시된 에러 메시지 사용 [cite: 31]
        super("로그인 시도 횟수를 초과했습니다. 관리자에게 문의하세요.");
        this.failedAttempts = failedAttempts;
    }
}