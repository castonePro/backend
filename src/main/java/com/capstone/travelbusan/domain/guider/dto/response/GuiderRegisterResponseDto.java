package com.capstone.travelbusan.domain.guider.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
public class GuiderRegisterResponseDto {
    private UUID guideId;
    private String message;

    public static GuiderRegisterResponseDto of(UUID guideId) {
        return GuiderRegisterResponseDto.builder()
                .guideId(guideId)
                .message("성공적으로 로컬 가이드로 등록되었습니다.")
                .build();
    }
}