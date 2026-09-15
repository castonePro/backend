package com.capstone.travelbusan.domain.route.exception;

/**
 * 외부 경로 탐색 API(네이버클라우드 Directions) 호출/응답 처리 중 발생한 오류를 나타낸다.
 * GlobalExceptionHandler에서 502 Bad Gateway로 변환되어 응답된다.
 */
public class RouteApiException extends RuntimeException {

    public RouteApiException(String message) {
        super(message);
    }

    public RouteApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
