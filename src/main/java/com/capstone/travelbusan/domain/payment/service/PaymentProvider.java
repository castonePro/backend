package com.capstone.travelbusan.domain.payment.service;

import java.math.BigDecimal;

/**
 * 결제(PG) 공급자 인터페이스.
 * 지금은 {@link MockPaymentProvider}만 구현되어 있고,
 * 추후 PortOne/토스페이먼츠 등 실제 PG로 교체할 때
 * 이 인터페이스만 새로 구현하면 서비스·컨트롤러 코드는 변경하지 않아도 된다.
 */
public interface PaymentProvider {

    /**
     * 결제를 승인 처리한다.
     * @param amount 결제 금액
     * @param description 결제 설명(수수료/보증금/부스트 등)
     * @return 이번 거래를 식별하는 transactionKey
     */
    String charge(BigDecimal amount, String description);

    /**
     * 결제를 환급 처리한다.
     * @param transactionKey charge()에서 발급된 거래 키
     * @param amount 환급 금액
     */
    void refund(String transactionKey, BigDecimal amount);
}
