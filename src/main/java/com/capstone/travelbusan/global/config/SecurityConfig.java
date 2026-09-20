package com.capstone.travelbusan.global.config;

import com.capstone.travelbusan.global.security.jwt.JwtAuthenticationFilter;
import com.capstone.travelbusan.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 적용
                .csrf(csrf -> csrf.disable()) // REST API이므로 CSRF 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) //무상태를 통한 세션 미사용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/places/**").permitAll()
                        .requestMatchers("/ws/**", "/ws/chat/**").permitAll()
                        .requestMatchers("/api/v1/auth/**", "/api/v1/planner/generate").permitAll()
                        // 멀티턴 대화도 비로그인 허용(기존 generate와 동일 정책).
                        // 대신 SessionRateLimiter(IP당 요청 수)와 세션당 턴·토큰 상한으로 남용을 막는다.
                        .requestMatchers("/api/v1/planner/sessions/**").permitAll()
                        //.requestMatchers("/api/v1/auth/**").permitAll() // 로그인/회원가입은 허용
                        .anyRequest().authenticated() // 나머지는 JWT 인증 필요
                )
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 배치
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 운영 도메인(travelbusan.site), 프론트엔드 배포 서버 (158.180.82.175) 및 Nginx 포트(80, 443, 3000 등), 로컬 개발 환경 허용
        configuration.setAllowedOriginPatterns(List.of(
                // 운영 도메인 (HTTPS)
                "https://travelbusan.site",
                "https://www.travelbusan.site",
                "http://158.180.82.175*",
                "https://158.180.82.175*",
                "http://localhost:[*]",
                "http://127.0.0.1:[*]"
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 모든 요청 헤더 허용
        configuration.setAllowedHeaders(List.of("*"));

        // 클라이언트에서 확인할 수 있도록 노출할 헤더
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));

        // 인증 정보(쿠키, Authorization 헤더 등) 포함 허용
        configuration.setAllowCredentials(true);

        // Preflight 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}