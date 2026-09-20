package com.capstone.travelbusan.domain.planner.exception;

/** 세션 턴 수·토큰 상한, 또는 IP 요청 한도를 넘었을 때. 컨트롤러가 429로 내려준다. */
public class PlannerLimitExceededException extends RuntimeException {
    public PlannerLimitExceededException(String message) {
        super(message);
    }
}
