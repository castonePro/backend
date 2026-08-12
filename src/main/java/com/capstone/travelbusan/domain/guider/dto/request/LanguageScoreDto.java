package com.capstone.travelbusan.domain.guider.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LanguageScoreDto {
    private String exam;   // 예: "TOEIC"
    private String score;  // 예: "900"
}