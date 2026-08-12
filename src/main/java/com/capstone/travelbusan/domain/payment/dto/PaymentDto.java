package com.capstone.travelbusan.domain.payment.dto;

import com.capstone.travelbusan.domain.payment.entity.Payment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentDto {

    @Getter
    @Builder
    public static class Response {
        private UUID paymentId;
        private UUID companionId;
        private String companionTitle;
        private String type;
        private BigDecimal amount;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
        private LocalDateTime resolvedAt;

        public static Response from(Payment payment) {
            return Response.builder()
                    .paymentId(payment.getPaymentId())
                    .companionId(payment.getCompanion().getCompanionId())
                    .companionTitle(payment.getCompanion().getTitle())
                    .type(payment.getType())
                    .amount(payment.getAmount())
                    .status(payment.getStatus())
                    .createdAt(payment.getCreatedAt())
                    .paidAt(payment.getPaidAt())
                    .resolvedAt(payment.getResolvedAt())
                    .build();
        }
    }
}
