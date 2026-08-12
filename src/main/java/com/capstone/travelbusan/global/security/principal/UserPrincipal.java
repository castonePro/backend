package com.capstone.travelbusan.global.security.principal;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {
    private final UUID userId; //
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(UUID userId, String email, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.authorities = authorities;
    }

    // 토큰 정보를 바탕으로 객체 생성
    public static UserPrincipal create(String userId, String email, boolean isGuide) {
        String role = isGuide ? "ROLE_GUIDE" : "ROLE_USER"; //
        return new UserPrincipal(
                UUID.fromString(userId),
                email,
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return null; } // JWT 방식이라 비밀번호는 무시
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
}
