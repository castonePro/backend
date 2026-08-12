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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // REST API이므로 CSRF 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) //무상태를 통한 세션 미사용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/places/**").permitAll()
                        .requestMatchers("/ws/chat/**").permitAll()
                        .requestMatchers("/api/v1/auth/**", "/api/v1/planner/generate").permitAll()
                        //.requestMatchers("/api/v1/auth/**").permitAll() // 로그인/회원가입은 허용
                        .anyRequest().authenticated() // 나머지는 JWT 인증 필요
                )
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 배치
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}