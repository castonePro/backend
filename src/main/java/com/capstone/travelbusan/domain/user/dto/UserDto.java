package com.capstone.travelbusan.domain.user.dto;

import java.time.LocalDateTime;

public class UserDto {

    // 내 프로필 조회 (본인 인증 상태 포함) — 마이페이지, 동행 방 개설/신청 진입 시 사용
    public record MeResponse(
            String user_id,
            String email,
            String nickname,
            String profile_image_url,
            boolean is_guide,
            boolean phone_verified,
            Integer birth_year,
            String gender,
            int companion_host_count,
            int companion_join_count,
            int no_show_count,
            String sanction_level,
            LocalDateTime restricted_until
    ) {}
}
