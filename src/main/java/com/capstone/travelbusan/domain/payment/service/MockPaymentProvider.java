package com.capstone.travelbusan.domain.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 개발용 Mock 결제 공급자.
 * 실제 카드 결제를 처리하지 않고, 요청이 오면 항상 즉시 승인 처리한다(가짜 거래 키 발급).
 * 프런트는 실제 PG 결제창 없이도 수수료/보증금/부스트 결제 플로우 전체를 테스트할 수 있다.
 *
 * 추후 PortOne 등으로 교체할 때는 이 클래스를 대체 구현으로 바꾸고
 * @Primary 또는 @Service 등록만 옮기면 된다. (PaymentProvider 인터페이스는 변경 없음)
 */
@Slf4j
@Service
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String charge(BigDecimal amount, String description) {
        String transactionKey = "MOCK-" + UUID.randomUUID();
        log.info("[MockPaymentProvider] 결제 승인 처리: {}원 · {} · txKey={}", amount, description, transactionKey);
        return transactionKey;
    }

    @Override
    public void refund(String transactionKey, BigDecimal amount) {
        log.info("[MockPaymentProvider] 환급 처리: {}원 · txKey={}", amount, transactionKey);
    }
}
