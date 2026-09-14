package com.capstone.travelbusan.global.exception;

import com.capstone.travelbusan.domain.route.exception.RouteApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    // 3. 필수 요청 파라미터 누락 (예: ?originLat= 없이 호출)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("필수 파라미터 누락: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400 Bad Request
                .body(ErrorResponse.error("필수 요청 파라미터가 없습니다: " + e.getParameterName()));
    }

    // 4. 요청 파라미터 타입 불일치 (예: originLat=abc 처럼 숫자 자리에 문자열)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("파라미터 타입 불일치: {} = {}", e.getName(), e.getValue());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400 Bad Request
                .body(ErrorResponse.error("요청 파라미터 형식이 올바르지 않습니다: " + e.getName()));
    }

    // 5. 외부 경로 탐색 API(네이버 Directions) 호출 실패
    @ExceptionHandler(RouteApiException.class)
    public ResponseEntity<ErrorResponse> handleRouteApiException(RouteApiException e) {
        log.error("경로 탐색 API 오류: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY) // 502 Bad Gateway (외부 API 연동 실패)
                .body(ErrorResponse.error(e.getMessage()));
    }

    // 6. 그 외 예상치 못한 모든 예외 (500 에러)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllException(Exception e) {
        log.error("서버 내부 예외 발생 (500): ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.error("서버 내부 오류가 발생했습니다: " + e.getMessage()));
    }
}