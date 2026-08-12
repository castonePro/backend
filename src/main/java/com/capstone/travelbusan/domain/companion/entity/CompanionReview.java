package com.capstone.travelbusan.domain.companion.entity;

import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 동행 종료 후 참여자 간 상호 평가.
 * 더블 블라인드: 같은 (companion, reviewer, reviewee) 쌍의 상대방이 답례 리뷰를 제출했거나,
 * companion.completedAt 로부터 7일이 지나야 서로에게 공개된다. (공개 판정은 CompanionReviewService에서 계산)
 */
@Entity
@Table(name = "companion_reviews")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CompanionReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_id")
    private UUID reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companion_id", nullable = false)
    private Companion companion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    // 1.0 ~ 5.0
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    // 예: "시간 약속", "매너 좋음", "소통 원활" 등 태그형 평가
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags;

    // 방장 전용 "일정 운영 평가" — reviewee가 해당 동행의 방장일 때만 값이 들어간다
    @Column(name = "operation_rating", precision = 2, scale = 1)
    private BigDecimal operationRating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
