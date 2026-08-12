package com.capstone.travelbusan.domain.guider.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class GuiderRegisterRequestDto {
    // 1단계
    private List<String> activeRegions;

    // 2단계
    private List<String> availableLanguages;
    private String experiencePeriod;
    private List<LanguageScoreDto> languageScores; // JSONB 매핑용 클래스

    // 3단계
    private String introduction;
    private List<String> specialties;
}