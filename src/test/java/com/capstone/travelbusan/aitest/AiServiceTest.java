package com.capstone.travelbusan.aitest;

import com.capstone.travelbusan.domain.ai.service.AiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AiServiceTest {

    @Autowired
    private AiService aiService;

    @Test
    @DisplayName("ChatGPT API가 정상적으로 응답을 반환하는지 테스트")
    void getChatResponseTest() {
        // Given
        String prompt = "부산 1박 2일 여행 코스 추천해줘.";

        // When
        String response = aiService.getChatResponse(prompt);

        // Then
        System.out.println("======= AI Response =======");
        System.out.println(response);
        System.out.println("===========================");

        assertThat(response).isNotNull();
        assertThat(response).contains("부산"); // 응답에 '부산'이라는 단어가 포함되었는지 확인
    }
}
