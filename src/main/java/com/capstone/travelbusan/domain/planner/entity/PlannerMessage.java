package com.capstone.travelbusan.domain.planner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 대화 한 줄. content에는 사용자 발화 또는 어시스턴트의 자연어 답변만 담는다.
 *
 * <p>일정 JSON은 여기에 쌓지 않는다. 과거 버전의 일정까지 히스토리에 남기면
 * 프롬프트 토큰이 턴마다 폭증하고, 모델이 옛 일정을 현재 일정으로 착각한다.
 * 일정은 {@link PlannerSession#getCurrentPlan()} 최신본 하나만 유지한다.
 *
 * <p>정렬은 created_at이 아니라 seq로 한다. 같은 턴의 user/assistant 두 줄이
 * 같은 밀리초에 저장될 수 있어서 시간만으로는 순서가 뒤집힐 수 있다.
 */
@Entity
@Table(name = "planner_messages")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlannerMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "message_id")
    private UUID messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PlannerSession session;

    /** 세션 안에서 1부터 증가하는 순번. */
    @Column(name = "seq", nullable = false)
    private Integer seq;

    /** "user" 또는 "assistant". */
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /** 이 턴의 분류 결과. 어시스턴트 메시지에만 채운다. */
    @Column(name = "intent", length = 20)
    private String intent;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
