package com.capstone.travelbusan.domain.guider.dto.response;

import com.capstone.travelbusan.domain.guider.dto.request.LanguageScoreDto;
import com.capstone.travelbusan.domain.guider.entity.GuiderInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GuiderProfileResponseDto {
    private String guideId;
    private String nickname;
    private String introduction;
    private List<String> activeRegions;
    private List<String> availableLanguages;
    private String experiencePeriod;
    private List<LanguageScoreDto> languageScores;
    private List<String> specialties;
    private Double certificationScore;
    private Double reviewScore;

    public static GuiderProfileResponseDto from(GuiderInfo guider) {
        return GuiderProfileResponseDto.builder()
                .guideId(guider.getGuideId().toString())
                .nickname(guider.getUser().getNickname())
                .introduction(guider.getIntroduction())
                .activeRegions(guider.getActiveRegions())
                .availableLanguages(guider.getAvailableLanguages())
                .experiencePeriod(guider.getExperiencePeriod())
                .languageScores(guider.getLanguageScores())
                .specialties(guider.getSpecialties())
                .certificationScore(guider.getCertificationScore().doubleValue())
                .reviewScore(guider.getReviewScore().doubleValue())
                .build();
    }
}
