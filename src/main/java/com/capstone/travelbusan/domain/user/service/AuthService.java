package com.capstone.travelbusan.domain.user.service;

import com.capstone.travelbusan.domain.user.dto.AuthDto;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import com.capstone.travelbusan.global.exception.LoginAttemptExceededException;
import com.capstone.travelbusan.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    private static final int MAX_FAILED_ATTEMPTS = 5;


    // 로그인 로직
    @Transactional(noRollbackFor = LoginAttemptExceededException.class)
    public AuthDto.AuthData login(AuthDto.LoginRequest request) {
        // 1. 사용자 조회 (계정이 없으면 일반적인 인증 실패 예외 발생)
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 2. 잠금 상태 확인
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new LoginAttemptExceededException(user.getFailedLoginAttempts());
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            // 실패 횟수 증가 및 즉시 DB 반영
            user.incrementFailedLoginAttempts();
            userRepository.save(user);

            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                throw new LoginAttemptExceededException(user.getFailedLoginAttempts());
            }
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 4. 인증 성공 시 실패 횟수 초기화
        user.resetFailedLoginAttempts();

        // 5. JWT 토큰 발급
        String accessToken = jwtProvider.createAccessToken(user.getId().toString(), user.getEmail(), user.isGuide());

        // 6. 명세서에 맞는 데이터 반환
        return new AuthDto.AuthData(
                user.getId().toString(),
                accessToken,
                user.getNickname(),
                user.isGuide()
        );
    }

    // 회원가입 로직
    @Transactional
    public AuthDto.SignUpResponse signUp(AuthDto.SignUpRequest request) {
        // 1. 이메일 중복 체크
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 비밀번호 암호화 후 유저 생성
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.create(request.email(), encodedPassword, request.nickname());

        // 3. DB 저장
        userRepository.save(user);

        return new AuthDto.SignUpResponse("success", "회원가입이 완료되었습니다.");
    }


}
