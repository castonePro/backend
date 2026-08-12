package com.capstone.travelbusan.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "social_provider", length = 50)
    private String socialProvider;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "is_guide", nullable = false)
    private boolean isGuide = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    // ── 본인 인증 / 동행 프로필 (Phase 0) ──

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "birth_year")
    private Integer birthYear;

    // "MALE" / "FEMALE" / "OTHER"
    @Column(name = "gender", length = 10)
    private String gender;

    // ── 동행 이력 (Phase 4) — 가이드 전환 심사(Phase 6)의 기초 자료로 쓰인다 ──

    @Column(name = "companion_host_count", nullable = false)
    private int companionHostCount = 0;

    @Column(name = "companion_join_count", nullable = false)
    private int companionJoinCount = 0;

    // ── 신뢰·안전 (Phase 5) ──

    public static final String SANCTION_NONE = "NONE";
    public static final String SANCTION_WARNED = "WARNED";
    public static final String SANCTION_RESTRICTED = "RESTRICTED";
    public static final String SANCTION_BANNED = "BANNED";

    private static final int RESTRICTION_DAYS = 14;

    @Column(name = "no_show_count", nullable = false)
    private int noShowCount = 0;

    @Column(name = "sanction_level", nullable = false, length = 20)
    private String sanctionLevel = SANCTION_NONE;

    // RESTRICTED 상태일 때만 값이 있다. 이 시각이 지나면 자동으로 제한이 풀린 것으로 간주(isSanctioned()가 false 반환).
    @Column(name = "restricted_until")
    private LocalDateTime restrictedUntil;

    // ── 가이드 전환 심사 (Phase 6) ──
    // 참여 3회·방장 3회·운영 평가 4.0 이상을 처음 만족한 시점에 true로 고정된다 (GuideConversionService에서 부여).
    @Column(name = "preliminary_guide", nullable = false)
    private boolean preliminaryGuide = false;

    // --- 비즈니스 메서드 ---

    // 0. 회원가입용 생성자
    public static User create(String email, String encodedPassword, String nickname) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.nickname = nickname;
        user.isGuide = false;
        user.failedLoginAttempts = 0;
        return user;
    }
    // 1. 로그인 실패 횟수 증가
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    // 2. 로그인 실패 횟수 초기화
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    // ★ 3. 가이드 권한 부여 플래그 수정 로직 (추가된 부분)
    public void promoteToGuide() {
        if (this.isGuide) {
            throw new IllegalStateException("이미 가이드 권한을 가진 사용자입니다.");
        }
        this.isGuide = true;
    }

    // 4. 본인 인증 완료 처리 (동행 방 개설·참여 신청의 선행 조건)
    public void verifyPhone(String phoneNumber, Integer birthYear, String gender) {
        this.phoneNumber = phoneNumber;
        this.birthYear = birthYear;
        this.gender = gender;
        this.phoneVerified = true;
    }

    // 5. 동행 완료 시 이력 카운트 증가 (CompanionService.complete()에서 호출)
    public void incrementCompanionHostCount() {
        this.companionHostCount++;
    }

    public void incrementCompanionJoinCount() {
        this.companionJoinCount++;
    }

    // 6. 노쇼 기록 — 1회차 경고, 2회차 14일 이용 제한, 3회차부터 영구 정지
    public void recordNoShow() {
        this.noShowCount++;
        if (this.noShowCount == 1) {
            this.sanctionLevel = SANCTION_WARNED;
        } else if (this.noShowCount == 2) {
            this.sanctionLevel = SANCTION_RESTRICTED;
            this.restrictedUntil = LocalDateTime.now().plusDays(RESTRICTION_DAYS);
        } else {
            this.sanctionLevel = SANCTION_BANNED;
            this.restrictedUntil = null;
        }
    }

    // 현재 시점 기준으로 실제 제재가 유효한지 (기간 제한은 기한이 지나면 자동 해제된 것으로 본다)
    public boolean isSanctioned() {
        if (SANCTION_BANNED.equals(this.sanctionLevel)) {
            return true;
        }
        if (SANCTION_RESTRICTED.equals(this.sanctionLevel)) {
            return this.restrictedUntil != null && LocalDateTime.now().isBefore(this.restrictedUntil);
        }
        return false;
    }

    // 7. 예비 가이드 배지 부여 — 조건을 처음 만족한 시점에 한 번만 호출된다 (이후 계속 true로 유지)
    public void grantPreliminaryGuide() {
        this.preliminaryGuide = true;
    }
}