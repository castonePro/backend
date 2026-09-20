package com.capstone.travelbusan.domain.planner.service;

import com.capstone.travelbusan.domain.planner.entity.PlannerMessage;
import com.capstone.travelbusan.domain.planner.entity.PlannerSession;
import com.capstone.travelbusan.domain.planner.repository.PlannerMessageRepository;
import com.capstone.travelbusan.domain.planner.repository.PlannerSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 턴 결과를 한 트랜잭션으로 쓰는 전용 빈.
 *
 * <p>같은 클래스 안에서 호출하면 {@code @Transactional}이 프록시를 타지 않아
 * 조용히 무시된다. 그래서 쓰기 경계를 별도 빈으로 분리했다.
 *
 * <p>쓰기만 짧게 잡는 이유는 {@code PlannerConversationService}쪽에 적어두었다.
 * OpenAI 호출 구간에 DB 커넥션이 물려 있으면 안 된다.
 */
@Service
@RequiredArgsConstructor
public class PlannerSessionStore {

    private final PlannerSessionRepository sessionRepository;
    private final PlannerMessageRepository messageRepository;

    @Transactional
    public PlannerSession saveTurn(UUID sessionId,
                                   String locale,
                                   String planJson,
                                   String userText,
                                   String reply,
                                   String intent,
                                   int promptTokens,
                                   int completionTokens) {

        PlannerSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException("대화 세션을 찾을 수 없습니다."));

        session.setLocale(locale);
        session.setCurrentPlan(planJson);
        session.recordUsage(promptTokens, completionTokens);

        int seq = messageRepository.countBySession_SessionId(sessionId);

        messageRepository.save(PlannerMessage.builder()
                .session(session)
                .seq(++seq)
                .role("user")
                .content(userText)
                .build());

        messageRepository.save(PlannerMessage.builder()
                .session(session)
                .seq(++seq)
                .role("assistant")
                .content(reply)
                .intent(intent)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .build());

        return sessionRepository.save(session);
    }
}
