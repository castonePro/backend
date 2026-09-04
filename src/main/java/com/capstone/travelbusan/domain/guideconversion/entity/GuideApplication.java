package com.capstone.travelbusan.domain.guideconversion.entity;

import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 정식 가이드 전환 신청. 예비 가이드(참여 3회·방장 3회·운영 평가 4.0 이상) 조건을 만족한 사용자만 신청할 수 있다.
 * 승인되어도 곧바로 가이드가 되는 것은 아니고, 기존 가이드 등록 폼(GuiderRegistrationService)을 통해
 * 프로필(지역·언어·소개)을 마저 입력해야 최종적으로 가이드가 된다 — 기존 가이드 등록 흐름은 그대로 둔다.
 */
@Entity
@Table(name = "guide_applications")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GuideApplication {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "application_id")
    private UUID applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Lob
    @Column(name = "message")
    private String message;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(name = "applied_at", updatable = false)
    @Builder.Default
    private LocalDateTime appliedAt = LocalDateTime.now();

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Lob
    @Column(name = "review_note")
    private String reviewNote;

    public void approve(String note) {
        if (!STATUS_PENDING.equals(this.status)) {
            throw new IllegalStateException("대기중인 신청만 승인할 수 있습니다.");
        }
        this.status = STATUS_APPROVED;
        this.reviewNote = note;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(String note) {
        if (!STATUS_PENDING.equals(this.status)) {
            throw new IllegalStateException("대기중인 신청만 거절할 수 있습니다.");
        }
        this.status = STATUS_REJECTED;
        this.reviewNote = note;
        this.reviewedAt = LocalDateTime.now();
    }
}
