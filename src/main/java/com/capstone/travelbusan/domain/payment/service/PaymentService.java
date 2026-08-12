package com.capstone.travelbusan.domain.payment.service;

import com.capstone.travelbusan.domain.companion.entity.Companion;
import com.capstone.travelbusan.domain.notification.service.FcmService;
import com.capstone.travelbusan.domain.payment.dto.PaymentDto;
import com.capstone.travelbusan.domain.payment.entity.Payment;
import com.capstone.travelbusan.domain.payment.repository.PaymentRepository;
import com.capstone.travelbusan.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 참여 수수료 · 보증금 · 모집글 부스트 결제.
 * 실제 결제는 {@link PaymentProvider}(현재 Mock)에 위임한다.
 *
 * 설계 메모: 기존 신청 승인(approve)·마감(close)·완료(complete)·취소(cancel) 흐름 자체는 결제 여부와
 * 무관하게 그대로 동작한다 (기존 기능 무수정 원칙). 이 서비스는 그 흐름에 "결제 요청 생성" 훅으로만
 * 얹혀 있고, 참여자가 결제를 미뤄도 기존 승인·채팅·완료 처리는 막히지 않는다. 실제 결제 강제(미납 시
 * 참여 제한 등)는 이번 범위에 포함하지 않았다 — Phase 7 확장 시 추가 검토 필요.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    public static final BigDecimal PARTICIPATION_FEE_AMOUNT = new BigDecimal("1500");
    public static final BigDecimal DEPOSIT_AMOUNT = new BigDecimal("7000");
    public static final BigDecimal BOOST_AMOUNT = new BigDecimal("3000");
    public static final int BOOST_DURATION_DAYS = 3;

    // 참여 수수료는 "3회차부터" 부과한다. completed 기준 참여 횟수(companionJoinCount)가
    // 이 값 이상이면 이번 신규 참여가 3번째 이상이라는 뜻이다.
    private static final int PARTICIPATION_FEE_FROM_JOIN_COUNT = 2;

    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;
    private final FcmService fcmService;

    // ==================== 참여자 결제 ====================

    // 방장이 신청을 승인했을 때 호출 — 3회차 이상 참여자에게만 수수료 결제 요청을 만든다.
    @Transactional
    public void requestParticipationFeeIfApplicable(User applicant, Companion companion) {
        if (applicant.getCompanionJoinCount() < PARTICIPATION_FEE_FROM_JOIN_COUNT) {
            return; // 1~2회차는 무료
        }
        if (paymentRepository.existsByCompanion_CompanionIdAndPayer_IdAndType(
                companion.getCompanionId(), applicant.getId(), Payment.TYPE_PARTICIPATION_FEE)) {
            return; // 이미 요청됨 (재승인 등 중복 방지)
        }

        Payment payment = Payment.builder()
                .payer(applicant)
                .companion(companion)
                .type(Payment.TYPE_PARTICIPATION_FEE)
                .amount(PARTICIPATION_FEE_AMOUNT)
                .build();
        paymentRepository.save(payment);

        fcmService.sendNotification(
                applicant.getId(),
                "참여 수수료 결제 안내",
                "'" + companion.getTitle() + "' 동행은 3회차 이상 참여로 참여 수수료 " + PARTICIPATION_FEE_AMOUNT + "원 결제가 필요해요."
        );
    }

    // 동행이 확정(CONFIRMED)됐을 때 호출 — 승인된 참여자 각각에게 보증금 결제 요청을 만든다.
    @Transactional
    public void requestDeposit(User participant, Companion companion) {
        if (paymentRepository.existsByCompanion_CompanionIdAndPayer_IdAndType(
                companion.getCompanionId(), participant.getId(), Payment.TYPE_DEPOSIT)) {
            return;
        }

        Payment payment = Payment.builder()
                .payer(participant)
                .companion(companion)
                .type(Payment.TYPE_DEPOSIT)
                .amount(DEPOSIT_AMOUNT)
                .build();
        paymentRepository.save(payment);

        fcmService.sendNotification(
                participant.getId(),
                "보증금 결제 안내",
                "'" + companion.getTitle() + "' 동행이 확정되었어요. 보증금 " + DEPOSIT_AMOUNT + "원을 결제해주세요."
        );
    }

    // 대기중인 내 결제(수수료/보증금 등) 실행
    @Transactional
    public PaymentDto.Response pay(UUID userId, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역을 찾을 수 없습니다."));
        if (!payment.getPayer().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 결제만 처리할 수 있습니다.");
        }

        String description = payment.getType() + " · " + payment.getCompanion().getTitle();
        String transactionKey = paymentProvider.charge(payment.getAmount(), description);
        payment.markPaid(transactionKey);

        return PaymentDto.Response.from(payment);
    }

    public List<PaymentDto.Response> getMyPayments(UUID userId) {
        return paymentRepository.findByPayer_IdOrderByCreatedAtDesc(userId).stream()
                .map(PaymentDto.Response::from)
                .toList();
    }

    // ==================== 완료/취소 시 정산 (CompanionService에서 호출) ====================

    // 여행 완료 — 결제된 보증금 전액 환급
    @Transactional
    public void refundDepositsOnComplete(Companion companion) {
        refundAll(companion, Payment.TYPE_DEPOSIT);
    }

    // 방장 취소 / 인원미달 취소 — 참여자 잘못이 아니므로 보증금·수수료 전액 환급
    @Transactional
    public void refundAllOnCancel(Companion companion) {
        refundAll(companion, Payment.TYPE_DEPOSIT);
        refundAll(companion, Payment.TYPE_PARTICIPATION_FEE);
    }

    private void refundAll(Companion companion, String type) {
        List<Payment> paid = paymentRepository.findByCompanion_CompanionIdAndTypeAndStatus(
                companion.getCompanionId(), type, Payment.STATUS_PAID);
        for (Payment payment : paid) {
            paymentProvider.refund(payment.getTransactionKey(), payment.getAmount());
            payment.markRefunded();
        }
    }

    // 노쇼 처리 시 — 해당 참여자의 보증금을 몰수(환급하지 않음)
    @Transactional
    public void forfeitDepositOnNoShow(Companion companion, User applicant) {
        paymentRepository.findByCompanion_CompanionIdAndPayer_IdAndType(
                        companion.getCompanionId(), applicant.getId(), Payment.TYPE_DEPOSIT)
                .filter(payment -> Payment.STATUS_PAID.equals(payment.getStatus()))
                .ifPresent(Payment::markForfeited);
    }

    // ==================== 모집글 부스트 ====================

    // 방장이 즉시 결제하며 부스트를 신청한다 (수수료/보증금과 달리 요청-결제가 한 번에 이뤄지는 능동 구매)
    @Transactional
    public PaymentDto.Response boost(User host, Companion companion) {
        String transactionKey = paymentProvider.charge(BOOST_AMOUNT, "모집글 부스트 · " + companion.getTitle());

        Payment payment = Payment.builder()
                .payer(host)
                .companion(companion)
                .type(Payment.TYPE_BOOST)
                .amount(BOOST_AMOUNT)
                .build();
        payment.markPaid(transactionKey);
        paymentRepository.save(payment);

        companion.applyBoost(LocalDateTime.now().plusDays(BOOST_DURATION_DAYS));

        return PaymentDto.Response.from(payment);
    }
}
