package com.capstone.travelbusan.domain.companion.entity;

import com.capstone.travelbusan.domain.planner.entity.Itinerary;
import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 동행 모집글.
 * 방장이 자신의 Itinerary(일정)를 바탕으로 동행을 모집하는 단위.
 * 기존 UserBid(가이드 마켓 - 일정 제안) 구조를 참고했으며, 별도 도메인으로 분리해
 * 가이드 마켓 기능(userbid/bidapplication)은 그대로 유지한다.
 */
@Entity
@Table(name = "companions")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Companion {

    // 상태값: RECRUITING(모집중) / UNDER_MINIMUM(마감·인원미달·방장결정대기)
    // / CONFIRMED(확정) / IN_PROGRESS(진행중) / COMPLETED(완료) / CANCELED(취소)
    public static final String STATUS_RECRUITING = "RECRUITING";
    public static final String STATUS_UNDER_MINIMUM = "UNDER_MINIMUM";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELED = "CANCELED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "companion_id")
    private UUID companionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "min_participants", nullable = false)
    private Integer minParticipants;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    // PostgreSQL text[] 매핑 (ItineraryDetail.categoryType과 동일 패턴)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "preference_tags", columnDefinition = "text[]")
    private List<String> preferenceTags;

    @Column(name = "cost_sharing_note", columnDefinition = "TEXT")
    private String costSharingNote;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 모집 나이대 — 둘 다 null이면 연령무관 (탐색 필터용)
    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    // 방장이 모집글에 남기는 인스타그램/커뮤니티 아이디 (선택) — "이번 여행은 이런 느낌" 소개용
    @Column(name = "sns_handle", length = 50)
    private String snsHandle;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_RECRUITING;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // 완료 시각 — Phase 4 더블 블라인드 리뷰 공개 기한(7일) 계산 기준
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 모집글 부스트 만료 시각 — 이 시각 이전이면 탐색 목록 상단에 노출된다 (Phase 7)
    @Column(name = "boosted_until")
    private LocalDateTime boostedUntil;

    // ── 상태 전이 ──

    public void closeForApplications(int approvedCount) {
        if (!STATUS_RECRUITING.equals(this.status)) {
            throw new IllegalStateException("모집중 상태에서만 마감할 수 있습니다.");
        }
        this.status = approvedCount >= this.minParticipants ? STATUS_CONFIRMED : STATUS_UNDER_MINIMUM;
    }

    // 최소 인원 미달 시 방장 결정: 소규모 진행 / 연기 / 취소
    public void applyUnderMinimumDecision(String decision, LocalDate newStartDate, LocalDate newEndDate) {
        if (!STATUS_UNDER_MINIMUM.equals(this.status)) {
            throw new IllegalStateException("인원 미달 상태가 아닙니다.");
        }
        switch (decision) {
            case "CONTINUE" -> this.status = STATUS_CONFIRMED;
            case "POSTPONE" -> {
                this.status = STATUS_RECRUITING;
                if (newStartDate != null) this.startDate = newStartDate;
                if (newEndDate != null) this.endDate = newEndDate;
            }
            case "CANCEL" -> this.status = STATUS_CANCELED;
            default -> throw new IllegalArgumentException("알 수 없는 결정 값입니다: " + decision);
        }
    }

    public void start() {
        if (!STATUS_CONFIRMED.equals(this.status)) {
            throw new IllegalStateException("확정 상태에서만 시작할 수 있습니다.");
        }
        this.status = STATUS_IN_PROGRESS;
    }

    public void complete() {
        if (!STATUS_IN_PROGRESS.equals(this.status)) {
            throw new IllegalStateException("진행중 상태에서만 완료할 수 있습니다.");
        }
        this.status = STATUS_COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (STATUS_COMPLETED.equals(this.status) || STATUS_CANCELED.equals(this.status)) {
            throw new IllegalStateException("이미 종료된 동행입니다.");
        }
        this.status = STATUS_CANCELED;
    }

    public boolean isRecruiting() {
        return STATUS_RECRUITING.equals(this.status);
    }

    // 모집글 부스트 적용 (Phase 7 — PaymentService.boost()에서 결제 성공 시 호출)
    public void applyBoost(LocalDateTime until) {
        this.boostedUntil = until;
    }

    public boolean isBoosted() {
        return this.boostedUntil != null && LocalDateTime.now().isBefore(this.boostedUntil);
    }
}
