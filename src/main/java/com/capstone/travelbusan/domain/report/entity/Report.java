package com.capstone.travelbusan.domain.report.entity;

import com.capstone.travelbusan.domain.companion.entity.Companion;
import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 신고. 채팅·평가·신청자 관리 화면 등에서 상대방을 신고할 때 생성된다.
 * 처리는 아직 자동화하지 않고(운영자 수동 검토 대상), 상태 필드만 관리한다.
 */
@Entity
@Table(name = "reports")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Report {

    // 신고 사유
    public static final String REASON_NO_SHOW = "NO_SHOW";
    public static final String REASON_HARASSMENT = "HARASSMENT";
    public static final String REASON_INAPPROPRIATE_BEHAVIOR = "INAPPROPRIATE_BEHAVIOR";
    public static final String REASON_FRAUD = "FRAUD";
    public static final String REASON_OTHER = "OTHER";

    // 처리 상태
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REVIEWED = "REVIEWED";
    public static final String STATUS_DISMISSED = "DISMISSED";
    public static final String STATUS_ACTION_TAKEN = "ACTION_TAKEN";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_id")
    private UUID reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    // 어떤 동행에서 있었던 일인지 (채팅에서 바로 신고하는 경우 등 문맥이 없을 수도 있어 nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companion_id")
    private Companion companion;

    @Column(name = "reason_category", nullable = false, length = 30)
    private String reasonCategory;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
