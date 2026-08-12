package com.capstone.travelbusan.domain.verification.service;

/**
 * 휴대폰 본인 인증 공급자 인터페이스.
 * 지금은 {@link MockPhoneVerificationProvider}만 구현되어 있고,
 * 추후 Firebase Phone Auth / PortOne 등 실제 PG로 교체할 때
 * 이 인터페이스만 새로 구현하면 서비스·컨트롤러 코드는 변경하지 않아도 된다.
 */
public interface PhoneVerificationProvider {

    /**
     * 인증번호를 발송(또는 생성)한다.
     * @param phoneNumber 인증 대상 휴대폰 번호
     * @return 이번 인증 시도를 식별하는 verificationId
     */
    VerificationIssue sendCode(String phoneNumber);

    /**
     * 인증번호가 유효한지 확인한다.
     * @param verificationId sendCode에서 발급된 식별자
     * @param code 사용자가 입력한 인증번호
     * @return 인증 성공 여부
     */
    boolean confirmCode(String verificationId, String code);

    /**
     * sendCode 결과.
     * @param verificationId 인증 시도 식별자
     * @param devCode 개발/Mock 단계에서만 채워지는 확인용 코드 (실 서비스 전환 시 null)
     */
    record VerificationIssue(String verificationId, String devCode) {}
}
