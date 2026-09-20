package com.capstone.travelbusan.domain.planner.controller;

import com.capstone.travelbusan.domain.ai.exception.AiApiException;
import com.capstone.travelbusan.domain.planner.dto.SessionCreateRequest;
import com.capstone.travelbusan.domain.planner.dto.SessionHistoryResponse;
import com.capstone.travelbusan.domain.planner.dto.SessionTurnRequest;
import com.capstone.travelbusan.domain.planner.dto.SessionTurnResponse;
import com.capstone.travelbusan.domain.planner.exception.PlannerLimitExceededException;
import com.capstone.travelbusan.domain.planner.service.PlannerConversationService;
import com.capstone.travelbusan.domain.planner.support.SessionRateLimiter;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 멀티턴 AI 플래너 대화 API.
 *
 * <p>기존 {@code POST /planner/generate}는 단발 생성용으로 그대로 둔다(하위 호환).
 * 대화형 클라이언트는 이쪽을 쓴다.
 *
 * <p>비로그인 사용자도 쓸 수 있게 열려 있으므로, 세션 안쪽 상한(턴 수·토큰)과
 * 바깥쪽 상한(IP당 요청 수)을 둘 다 건다. 하나만으로는 세션을 계속 새로 만드는
 * 남용을 막을 수 없다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/planner/sessions")
@RequiredArgsConstructor
public class PlannerSessionController {

    private final PlannerConversationService conversationService;
    private final SessionRateLimiter rateLimiter;

    /** 새 대화 시작. */
    @PostMapping
    public ResponseEntity<?> createSession(@RequestBody(required = false) SessionCreateRequest request,
                                           @AuthenticationPrincipal UserPrincipal currentUser,
                                           HttpServletRequest http) {
        return guarded(http, () ->
                ResponseEntity.ok(conversationService.createSession(request, userIdOf(currentUser))));
    }

    /** 대화 한 턴. 의도에 따라 새 일정 생성 / 수정 / 질문 답변으로 갈린다. */
    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable("sessionId") String sessionId,
                                         @RequestBody SessionTurnRequest request,
                                         @AuthenticationPrincipal UserPrincipal currentUser,
                                         HttpServletRequest http) {
        return guarded(http, () -> {
            SessionTurnResponse response = conversationService.handleTurn(
                    parseSessionId(sessionId), request, userIdOf(currentUser));
            return ResponseEntity.ok(response);
        });
    }

    /** 히스토리 복구. 새로고침해도 대화와 일정이 살아나도록. */
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable("sessionId") String sessionId,
                                        @AuthenticationPrincipal UserPrincipal currentUser) {
        try {
            SessionHistoryResponse response = conversationService.getHistory(
                    parseSessionId(sessionId), userIdOf(currentUser));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return toErrorResponse(e);
        }
    }

    // ───────────────────────── 공통 처리 ─────────────────────────

    private ResponseEntity<?> guarded(HttpServletRequest http, Supplier<ResponseEntity<?>> action) {
        String clientKey = clientKey(http);
        if (!rateLimiter.tryAcquire(clientKey)) {
            return ResponseEntity.status(429).body(error(
                    "요청이 너무 잦습니다. %d분 후 다시 시도해 주세요."
                            .formatted(rateLimiter.windowMinutes())));
        }
        try {
            return action.get();
        } catch (Exception e) {
            return toErrorResponse(e);
        }
    }

    private ResponseEntity<?> toErrorResponse(Exception e) {
        if (e instanceof PlannerLimitExceededException) {
            log.info("세션 상한 도달: {}", e.getMessage());
            return ResponseEntity.status(429).body(error(e.getMessage()));
        }
        if (e instanceof AccessDeniedException) {
            return ResponseEntity.status(403).body(error("이 대화에 접근할 권한이 없습니다."));
        }
        if (e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
        if (e instanceof IllegalStateException) {
            log.warn("대화 처리 실패(입력/데이터 문제): {}", e.getMessage());
            return ResponseEntity.unprocessableEntity().body(error(e.getMessage()));
        }
        if (e instanceof AiApiException) {
            log.error("대화 처리 실패(AI 호출): ", e);
            return ResponseEntity.status(502).body(error("AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요."));
        }
        log.error("대화 처리 중 오류 발생: ", e);
        return ResponseEntity.internalServerError().body(error("대화 처리 중 오류가 발생했습니다."));
    }

    private static Map<String, String> error(String message) {
        return Map.of("message", (message == null || message.isBlank())
                ? "알 수 없는 오류가 발생했습니다." : message);
    }

    private static UUID parseSessionId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("올바르지 않은 세션 식별자입니다.");
        }
    }

    private static UUID userIdOf(UserPrincipal currentUser) {
        return (currentUser != null) ? currentUser.getUserId() : null;
    }

    /** 프록시 뒤에서는 X-Forwarded-For의 첫 주소가 실제 클라이언트 IP다. */
    private static String clientKey(HttpServletRequest http) {
        if (http == null) return "unknown";
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0) ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return http.getRemoteAddr();
    }
}
