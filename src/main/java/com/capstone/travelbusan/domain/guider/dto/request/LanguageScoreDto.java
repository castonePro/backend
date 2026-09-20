package com.capstone.travelbusan.domain.guider.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LanguageScoreDto {

    @JsonProperty("language")
    @JsonAlias({"lang"})
    private String language; // 예: "영어"

    @JsonProperty("testName")
    @JsonAlias({"exam", "test_name", "examName", "exam_name"})
    private String testName; // 예: "TOEIC"

    @JsonProperty("score")
    @JsonAlias({"testScore", "score_value", "level"})
    private String score;    // 예: "850"

    // 기존 2개 인자 생성자 호환용 (exam/testName, score)
    public LanguageScoreDto(String testName, String score) {
        this.testName = testName;
        this.score = score;
    }

    // 기존 exam 필드명과의 호환성 유지용 getter/setter
    public String getExam() {
        return this.testName;
    }

    public void setExam(String exam) {
        this.testName = exam;
    }
}