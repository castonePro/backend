package com.capstone.travelbusan.domain.planner.service;

import com.capstone.travelbusan.domain.ai.dto.AiChatOptions;
import com.capstone.travelbusan.domain.ai.dto.AiJsonResult;
import com.capstone.travelbusan.domain.ai.dto.AiMessage;
import com.capstone.travelbusan.domain.ai.service.AiService;
import com.capstone.travelbusan.domain.planner.dto.*;
import com.capstone.travelbusan.domain.planner.entity.PlannerMessage;
import com.capstone.travelbusan.domain.planner.entity.PlannerSession;
import com.capstone.travelbusan.domain.planner.exception.PlannerLimitExceededException;
import com.capstone.travelbusan.domain.planner.repository.PlannerMessageRepository;
import com.capstone.travelbusan.domain.planner.repository.PlannerSessionRepository;
import com.capstone.travelbusan.domain.planner.support.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 멀티턴 대화 오케스트레이션.
 *
 * <p>턴 하나의 흐름:
 * <ol>
 *   <li>저렴한 1차 호출로 의도를 분류한다. ASK·OUT_OF_SCOPE는 여기서 답변까지 끝나므로
 *       RAG 검색도 2차 호출도 일어나지 않는다. 비용 절감의 대부분이 여기서 나온다.</li>
 *   <li>NEW_PLAN이면 기존 생성 파이프라인을 그대로 태운다.</li>
 *   <li>MODIFY면 전체를 다시 만들지 않고 patch 연산만 받아 현재 일정에 적용한다.</li>
 * </ol>
 *
 * <p>메서드 전체를 트랜잭션으로 감싸지 않는다. OpenAI 호출이 수십 초 걸릴 수 있는데
 * 그동안 DB 커넥션을 붙잡고 있으면 커넥션 풀이 마른다. 읽기와 쓰기만 각자 짧게 잡는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlannerConversationService {

    /** 세션당 최대 턴. 익명 세션이 무한히 늘어나는 걸 막는 1차 방어선. */
    private static final int MAX_TURNS_PER_SESSION = 20;
    /** 세션당 누적 토큰 상한. 토큰이 비용이므로 여기가 실질적인 지갑 방어선이다. */
    private static final int MAX_TOKENS_PER_SESSION = 150_000;
    /** 수정 턴에서 새로 넣을 수 있는 장소 후보 수. 생성 턴(30개)보다 적게 잡아 토큰을 아낀다. */
    private static final int MODIFY_CANDIDATE_LIMIT = 15;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AiService aiService;
    private final PlannerService plannerService;
    private final PlannerSessionRepository sessionRepository;
    private final PlannerMessageRepository messageRepository;
    private final PlannerSessionStore sessionStore;
    private final ObjectMapper objectMapper;

    // ───────────────────────── 세션 수명 ─────────────────────────

    @Transactional
    public SessionHistoryResponse createSession(SessionCreateRequest request, UUID userId) {
        PlannerSession session = PlannerSession.builder()
                .userId(userId)
                .locale((request != null) ? request.lang() : null)
                .build();
        sessionRepository.save(session);
        log.info("플래너 세션 생성: sessionId={}, userId={}", session.getSessionId(), userId);

        return new SessionHistoryResponse(
                session.getSessionId().toString(),
                session.getLocale(),
                List.of(),
                null,
                0,
                MAX_TURNS_PER_SESSION);
    }

    @Transactional(readOnly = true)
    public SessionHistoryResponse getHistory(UUID sessionId, UUID userId) {
        PlannerSession session = loadSession(sessionId, userId);

        List<SessionMessageDto> messages = messageRepository
                .findBySession_SessionIdOrderBySeqAsc(sessionId).stream()
                .map(m -> new SessionMessageDto(
                        m.getSeq(),
                        m.getRole(),
                        m.getContent(),
                        m.getIntent(),
                        (m.getCreatedAt() != null) ? m.getCreatedAt().format(TS_FMT) : null))
                .toList();

        return new SessionHistoryResponse(
                session.getSessionId().toString(),
                session.getLocale(),
                messages,
                readPlan(session),
                turnCount(session),
                remainingTurns(session));
    }

    // ───────────────────────── 턴 처리 ─────────────────────────

    public SessionTurnResponse handleTurn(UUID sessionId, SessionTurnRequest request, UUID userId) {
        String text = (request != null && request.text() != null) ? request.text().trim() : "";
        if (text.isEmpty()) {
            throw new IllegalArgumentException("메시지가 비어 있습니다.");
        }

        PlannerSession session = loadSession(sessionId, userId);
        assertWithinLimits(session);

        String locale = firstNonBlank(
                (request != null) ? request.lang() : null,
                session.getLocale());
        String language = PlannerPromptFactory.languageName(locale);

        GeneratedPlanDto currentPlan = readPlan(session);
        // 사용자 메시지를 저장하기 "전에" 히스토리를 읽는다. 방금 발화가 중복으로 들어가지 않도록.
        List<AiMessage> history = recentHistory(sessionId);

        // ── 1차: 의도 분류 ──
        List<AiMessage> intentMessages = new ArrayList<>();
        intentMessages.add(ConversationPromptFactory.intentSystemMessage(language, currentPlan != null));
        intentMessages.addAll(history);
        if (currentPlan != null) {
            intentMessages.add(ConversationPromptFactory.planMessage(currentPlan));
        }
        intentMessages.add(ConversationPromptFactory.utteranceMessage(text));

        AiJsonResult<LlmIntentDecision> intentResult = aiService.chatAsJsonWithUsage(
                intentMessages,
                AiChatOptions.defaults()
                        .withTemperature(0)
                        .withMaxTokens(500)
                        .withResponseSchema(ConversationPromptFactory.intentSchema()),
                LlmIntentDecision.class);

        int promptTokens = intentResult.raw().promptTokens();
        int completionTokens = intentResult.raw().completionTokens();

        PlannerIntent intent = PlannerIntent.from(
                intentResult.value().intent(),
                (currentPlan == null) ? PlannerIntent.NEW_PLAN : PlannerIntent.MODIFY);
        if (intent == PlannerIntent.MODIFY && currentPlan == null) {
            // 고칠 일정이 없으면 수정이 성립하지 않는다
            intent = PlannerIntent.NEW_PLAN;
        }
        log.info("턴 분류: sessionId={}, intent={}", sessionId, intent);

        // ── 2차: 의도별 처리 ──
        String reply = nullToEmpty(intentResult.value().reply());
        GeneratedPlanDto plan = currentPlan;
        List<Long> changed = List.of();

        switch (intent) {
            case NEW_PLAN -> {
                PlanGeneration generated = plannerService.generateTravelPlan(
                        new PlannerRequest(text, (request != null) ? request.categories() : null, locale));
                plan = generated.plan();
                reply = nullToEmpty(generated.reply());
                promptTokens += generated.promptTokens();
                completionTokens += generated.completionTokens();
                changed = plan.generated_courses().stream()
                        .map(GeneratedCourseDto::place_id)
                        .filter(Objects::nonNull)
                        .toList();
            }
            case MODIFY -> {
                ModifyOutcome outcome = modify(currentPlan, text, language, history);
                plan = outcome.plan();
                reply = outcome.reply();
                changed = outcome.changedPlaceIds();
                promptTokens += outcome.promptTokens();
                completionTokens += outcome.completionTokens();
            }
            case ASK, OUT_OF_SCOPE -> {
                // 1차 호출이 이미 답변을 만들었다. 일정은 손대지 않는다.
            }
        }

        return persistTurn(sessionId, locale, text, reply, intent, plan, changed, promptTokens, completionTokens);
    }

    private record ModifyOutcome(
            GeneratedPlanDto plan,
            String reply,
            List<Long> changedPlaceIds,
            int promptTokens,
            int completionTokens
    ) {}

    private ModifyOutcome modify(GeneratedPlanDto currentPlan,
                                 String text,
                                 String language,
                                 List<AiMessage> history) {
        // 새 장소를 넣을 수 있어야 하므로 후보를 다시 뽑는다. 생성 턴보다 적게.
        List<PlaceCandidate> candidates = plannerService.findCandidates(text, MODIFY_CANDIDATE_LIMIT);
        Map<Long, PlaceCandidate> byId = new LinkedHashMap<>();
        for (PlaceCandidate c : candidates) {
            byId.put(c.placeId(), c);
        }

        List<AiMessage> messages = new ArrayList<>();
        messages.add(ConversationPromptFactory.patchSystemMessage(language));
        messages.addAll(history);
        messages.add(ConversationPromptFactory.planMessage(currentPlan));
        messages.add(PlannerPromptFactory.candidatesMessage(candidates));
        messages.add(ConversationPromptFactory.utteranceMessage(text));

        AiJsonResult<LlmPatchResult> result = aiService.chatAsJsonWithUsage(
                messages,
                AiChatOptions.defaults()
                        .withTemperature(0.2)
                        .withMaxTokens(1500)
                        .withResponseSchema(ConversationPromptFactory.patchSchema()),
                LlmPatchResult.class);

        int in = result.raw().promptTokens();
        int out = result.raw().completionTokens();
        String reply = nullToEmpty(result.value().reply());

        try {
            PlanPatcher.PatchOutcome patched =
                    PlanPatcher.apply(currentPlan, result.value().operations(), byId);
            log.info("patch 적용: 반영 {}건, 거절 {}건", patched.applied(), patched.rejected());
            return new ModifyOutcome(patched.plan(), reply, patched.changedPlaceIds(), in, out);
        } catch (IllegalStateException e) {
            // 일정이 비게 되는 등 적용할 수 없는 변경. 기존 일정을 지키고 사실대로 알린다.
            log.warn("patch 적용 실패, 기존 일정을 유지합니다: {}", e.getMessage());
            return new ModifyOutcome(currentPlan, e.getMessage(), List.of(), in, out);
        }
    }

    // ───────────────────────── 저장 ─────────────────────────

    private SessionTurnResponse persistTurn(UUID sessionId,
                                            String locale,
                                            String userText,
                                            String reply,
                                            PlannerIntent intent,
                                            GeneratedPlanDto plan,
                                            List<Long> changed,
                                            int promptTokens,
                                            int completionTokens) {

        PlannerSession saved = sessionStore.saveTurn(
                sessionId, locale, writePlan(plan), userText, reply, intent.name(),
                promptTokens, completionTokens);

        return new SessionTurnResponse(
                saved.getSessionId().toString(),
                intent.name(),
                reply,
                plan,
                changed,
                turnCount(saved),
                remainingTurns(saved));
    }

    // ───────────────────────── 보조 ─────────────────────────

    private PlannerSession loadSession(UUID sessionId, UUID userId) {
        PlannerSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("대화 세션을 찾을 수 없습니다."));
        // 익명 세션은 세션 id를 아는 사람만 접근한다(UUID라 추측 불가).
        // 로그인 사용자가 만든 세션은 본인만 접근할 수 있다.
        if (session.getUserId() != null && !session.getUserId().equals(userId)) {
            throw new AccessDeniedException("이 대화에 접근할 권한이 없습니다.");
        }
        return session;
    }

    private void assertWithinLimits(PlannerSession session) {
        if (turnCount(session) >= MAX_TURNS_PER_SESSION) {
            throw new PlannerLimitExceededException(
                    "이 대화의 턴 수 상한(" + MAX_TURNS_PER_SESSION + "회)에 도달했습니다. 새 대화를 시작해 주세요.");
        }
        if (session.totalTokens() >= MAX_TOKENS_PER_SESSION) {
            throw new PlannerLimitExceededException(
                    "이 대화가 사용할 수 있는 분량을 모두 썼습니다. 새 대화를 시작해 주세요.");
        }
    }

    /** 프롬프트에 넣을 최근 대화. 오래된 턴까지 전부 넣으면 토큰이 턴마다 선형으로 늘어난다. */
    private List<AiMessage> recentHistory(UUID sessionId) {
        List<PlannerMessage> recent = messageRepository.findTop12BySession_SessionIdOrderBySeqDesc(sessionId);
        List<AiMessage> messages = new ArrayList<>(recent.size());
        for (int i = recent.size() - 1; i >= 0; i--) {
            PlannerMessage m = recent.get(i);
            if (m.getContent() == null || m.getContent().isBlank()) continue;
            messages.add("assistant".equals(m.getRole())
                    ? AiMessage.assistant(m.getContent())
                    : AiMessage.user(m.getContent()));
        }
        return messages;
    }

    private GeneratedPlanDto readPlan(PlannerSession session) {
        String json = session.getCurrentPlan();
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, GeneratedPlanDto.class);
        } catch (Exception e) {
            log.error("저장된 일정 JSON을 읽지 못했습니다: sessionId={}", session.getSessionId(), e);
            return null;
        }
    }

    private String writePlan(GeneratedPlanDto plan) {
        if (plan == null) return null;
        try {
            return objectMapper.writeValueAsString(plan);
        } catch (Exception e) {
            throw new IllegalStateException("일정을 저장 형식으로 변환하지 못했습니다.", e);
        }
    }

    private static int turnCount(PlannerSession session) {
        return (session.getTurnCount() == null) ? 0 : session.getTurnCount();
    }

    private static int remainingTurns(PlannerSession session) {
        return Math.max(0, MAX_TURNS_PER_SESSION - turnCount(session));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return (b != null && !b.isBlank()) ? b : null;
    }

    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }
}
