package com.capstone.travelbusan.global.security.jwt;

import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long validityInMilliseconds;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-in-seconds}") long validityInSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInSeconds * 1000;
    }

    // 1. 토큰 생성: 명세에 따른 클레임 포함
    public String createAccessToken(String userId, String email, boolean isGuide) {
        Claims claims = Jwts.claims()
                .subject(userId)
                .add("email", email)
                .add("is_guide", isGuide)
                .build();

        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validityInMilliseconds))
                .signWith(secretKey)
                .compact();
    }

    // 2. 핵심: Filter에서 호출할 인증 객체 생성 메서드
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        // 토큰에 담긴 정보로 UserPrincipal 생성
        UserPrincipal principal = UserPrincipal.create(
                claims.getSubject(),
                claims.get("email", String.class),
                claims.get("is_guide", Boolean.class)
        );

        // Spring Security의 표준 인증 토큰 반환
        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }

    // 3. 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // 내부 메서드: Claims 파싱
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}