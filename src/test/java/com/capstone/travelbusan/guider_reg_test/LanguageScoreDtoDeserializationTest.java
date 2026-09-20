package com.capstone.travelbusan.guider_reg_test;

import com.capstone.travelbusan.domain.guider.dto.request.LanguageScoreDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageScoreDtoDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    @DisplayName("Oracle DB의 실제 guider_info.language_scores JSON 데이터가 정상 역직렬화된다")
    void deserializeActualDbJson() throws Exception {
        String dbJson = """
            [{"language":"영어","testName":"TOEIC","score":"850"}]
        """;

        List<LanguageScoreDto> list = mapper.readValue(dbJson, new TypeReference<>() {});

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getLanguage()).isEqualTo("영어");
        assertThat(list.get(0).getTestName()).isEqualTo("TOEIC");
        assertThat(list.get(0).getExam()).isEqualTo("TOEIC");
        assertThat(list.get(0).getScore()).isEqualTo("850");
    }

    @Test
    @DisplayName("레거시 exam 필드명이 들어와도 testName에 정상 매핑된다")
    void deserializeWithExamField() throws Exception {
        String legacyJson = """
            [{"exam":"JLPT","score":"N1"}]
        """;

        List<LanguageScoreDto> list = mapper.readValue(legacyJson, new TypeReference<>() {});

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTestName()).isEqualTo("JLPT");
        assertThat(list.get(0).getExam()).isEqualTo("JLPT");
        assertThat(list.get(0).getScore()).isEqualTo("N1");
    }
}
