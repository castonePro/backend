package com.capstone.travelbusan.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 로그인 시도 초과 예외 처리
    @ExceptionHandler(LoginAttemptExceededException.class)
    public ResponseEntity<ErrorResponse> handleLoginAttemptExceeded(LoginAttemptExceededException e) {
        log.warn("로그인 시도 초과: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // 403 Forbidden
                .body(ErrorResponse.loginFail(e.getMessage(), e.getFailedAttempts()));
    }

    // 2. 잘못된 인자 예외
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 인자 예외: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400 Bad Request
                .body(ErrorResponse.error(e.getMessage()));
    }

    // 3. 그 외 예상치 못한 모든 예외 (500 에러)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllException(Exception e) {
        log.error("서버 내부 예외 발생 (500): ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.error("서버 내부 오류가 발생했습니다: " + e.getMessage()));
    }
}