package com.capstone.travelbusan.domain.companion.entity;

import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 동행 참여 신청. 승인제 — 방장이 approve/reject 한다.
 */
// 신청 이력을 남기기 위해 취소/거절해도 행을 삭제하지 않는다(soft-cancel).
// 그래서 (companion_id, applicant_id)에 DB 유니크 제약을 걸지 않고,
// "PENDING/APPROVED 같은 활성 신청이 있는지"는 서비스 레이어에서 검증한다.
@Entity
@Table(name = "companion_applications")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CompanionApplication {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELED = "CANCELED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "application_id")
    private UUID applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companion_id", nullable = false)
    private Companion companion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Lob
    @Column(name = "introduction")
    private String introduction;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // 노쇼 처리 여부 — 출발 임박(48시간 이내) 취소 또는 방장의 수동 노쇼 처리로 true가 된다.
    // Phase 5: 신뢰·안전. 이력 표시용으로 상태(status)와 별개로 관리한다.
    @Column(name = "no_show", nullable = false)
    @Builder.Default
    private boolean noShow = false;

    public void approve() {
        if (!STATUS_PENDING.equals(this.status)) {
            throw new IllegalStateException("대기중인 신청만 승인할 수 있습니다.");
        }
        this.status = STATUS_APPROVED;
    }

    public void reject() {
        if (!STATUS_PENDING.equals(this.status)) {
            throw new IllegalStateException("대기중인 신청만 거절할 수 있습니다.");
        }
        this.status = STATUS_REJECTED;
    }

    public void cancel() {
        if (STATUS_REJECTED.equals(this.status)) {
            throw new IllegalStateException("이미 거절된 신청입니다.");
        }
        this.status = STATUS_CANCELED;
    }

    public void markNoShow() {
        if (this.noShow) {
            throw new IllegalStateException("이미 노쇼로 처리된 신청입니다.");
        }
        this.noShow = true;
    }
}
