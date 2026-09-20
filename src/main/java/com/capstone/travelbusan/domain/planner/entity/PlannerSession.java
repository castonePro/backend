package com.capstone.travelbusan.domain.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 플래너 대화 세션.
 *
 * <p>{@code currentPlan}(일정 JSON)이 이 대화의 single source of truth다.
 * 턴마다 LLM이 일정을 처음부터 다시 만드는 게 아니라, 여기 저장된 일정을 읽어서
 * 변경(patch)만 적용한다. 그래서 사용자가 건드리지 않은 날은 그대로 유지된다.
 *
 * <p>user_id는 nullable이다. 지금 생성 엔드포인트가 비로그인 허용이라 익명 세션이
 * 만들어질 수 있고, 그래서 남용 방지 가드(턴 수·토큰 상한)가 함께 필요하다.
 */
@Entity
@Table(name = "planner_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlannerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "session_id", length = 36)
    private UUID sessionId;

    /** 로그인 사용자의 id. 익명 세션이면 null. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_id", length = 36)
    private UUID userId;

    @Column(name = "locale", length = 16)
    private String locale;

    /** 현재 일정(GeneratedPlanDto)의 JSON 직렬화. 아직 일정이 없으면 null. */
    @Lob
    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(name = "current_plan")
    private String currentPlan;

    /**
     * 오래된 턴을 대신하는 선호 요약("해산물 선호, 도보 이동 선호" 같은).
     * 히스토리를 무한히 쌓지 않으면서 사용자의 응답을 계속 반영하기 위한 슬롯.
     */
    @Lob
    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(name = "preference_summary")
    private String preferenceSummary;

    @Column(name = "turn_count", nullable = false)
    @Builder.Default
    private Integer turnCount = 0;

    @Column(name = "prompt_tokens", nullable = false)
    @Builder.Default
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens", nullable = false)
    @Builder.Default
    private Integer completionTokens = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public int totalTokens() {
        return (promptTokens == null ? 0 : promptTokens) + (completionTokens == null ? 0 : completionTokens);
    }

    public void recordUsage(int prompt, int completion) {
        this.promptTokens = (this.promptTokens == null ? 0 : this.promptTokens) + prompt;
        this.completionTokens = (this.completionTokens == null ? 0 : this.completionTokens) + completion;
        this.turnCount = (this.turnCount == null ? 0 : this.turnCount) + 1;
        this.updatedAt = LocalDateTime.now();
    }
}
