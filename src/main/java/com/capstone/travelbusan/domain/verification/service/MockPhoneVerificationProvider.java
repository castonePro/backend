package com.capstone.travelbusan.domain.verification.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 개발용 Mock 본인 인증 공급자.
 * 실제 SMS를 발송하지 않고, 서버가 인증번호를 생성해 응답에 그대로 실어 보낸다(devCode).
 * 프런트는 이 devCode를 화면에 노출해 실제 PG 없이도 인증 플로우 전체를 테스트할 수 있다.
 *
 * 추후 Firebase Phone Auth 등으로 교체할 때는 이 클래스를 대체 구현으로 바꾸고
 * @Primary 또는 @Service 등록만 옮기면 된다. (PhoneVerificationProvider 인터페이스는 변경 없음)
 */
@Service
public class MockPhoneVerificationProvider implements PhoneVerificationProvider {

    private static final SecureRandom RANDOM = new SecureRandom();

    // verificationId -> 발급된 인증번호 (인메모리, 서버 재시작 시 초기화됨 — 개발 단계에서만 사용)
    private final Map<String, String> issuedCodes = new ConcurrentHashMap<>();

    @Override
    public VerificationIssue sendCode(String phoneNumber) {
        String verificationId = UUID.randomUUID().toString();
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        issuedCodes.put(verificationId, code);
        return new VerificationIssue(verificationId, code);
    }

    @Override
    public boolean confirmCode(String verificationId, String code) {
        String issued = issuedCodes.get(verificationId);
        if (issued == null) {
            return false;
        }
        boolean matched = issued.equals(code);
        if (matched) {
            issuedCodes.remove(verificationId); // 1회용
        }
        return matched;
    }
}
