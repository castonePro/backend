package com.capstone.travelbusan.domain.payment.entity;

import com.capstone.travelbusan.domain.companion.entity.Companion;
import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제(수수료·보증금·부스트) 이력. 실제 PG 연동 전, {@link com.capstone.travelbusan.domain.payment.service.PaymentProvider}
 * 를 통해 처리되며 지금은 Mock 구현을 쓴다 (Phase 0 본인 인증과 동일한 패턴).
 */
@Entity
@Table(name = "payments")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Payment {

    // 결제 유형
    public static final String TYPE_PARTICIPATION_FEE = "PARTICIPATION_FEE"; // 참여 수수료 (3회차부터)
    public static final String TYPE_DEPOSIT = "DEPOSIT";                     // 보증금
    public static final String TYPE_BOOST = "BOOST";                        // 모집글 부스트

    // 상태
    public static final String STATUS_PENDING = "PENDING";     // 결제 대기
    public static final String STATUS_PAID = "PAID";           // 결제 완료
    public static final String STATUS_REFUNDED = "REFUNDED";   // 환급 완료
    public static final String STATUS_FORFEITED = "FORFEITED"; // 몰수(노쇼 등, 환급하지 않음)
    public static final String STATUS_FAILED = "FAILED";       // 결제 실패

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private UUID paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    // 어떤 동행에 대한 결제인지 (부스트는 방장 본인 소유 동행, 수수료/보증금은 참여자 본인의 참여 건)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companion_id", nullable = false)
    private Companion companion;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = STATUS_PENDING;

    // Mock PG가 발급하는 가짜 거래 키. 실제 PG 연동 시 이 필드에 실제 paymentKey/tid가 들어간다.
    @Column(name = "transaction_key", length = 100)
    private String transactionKey;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt; // 환급/몰수 처리 시각

    public void markPaid(String transactionKey) {
        if (!STATUS_PENDING.equals(this.status)) {
            throw new IllegalStateException("결제 대기 상태가 아닙니다.");
        }
        this.status = STATUS_PAID;
        this.transactionKey = transactionKey;
        this.paidAt = LocalDateTime.now();
    }

    public void markRefunded() {
        if (!STATUS_PAID.equals(this.status)) {
            throw new IllegalStateException("결제 완료 상태만 환급할 수 있습니다.");
        }
        this.status = STATUS_REFUNDED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markForfeited() {
        if (!STATUS_PAID.equals(this.status)) {
            throw new IllegalStateException("결제 완료 상태만 몰수할 수 있습니다.");
        }
        this.status = STATUS_FORFEITED;
        this.resolvedAt = LocalDateTime.now();
    }
}
