package com.capstone.travelbusan.domain.ai.service;

import org.springframework.beans.factory.annotation.Value; // Spring용으로 변경
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getChatResponse(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";

        // 1. 헤더 설정 (org.springframework.http.HttpHeaders 사용)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 2. 바디 설정 (간단하게 Map이나 Record 사용)
        // 모델은 말씀하신 가장 가벼운 gpt-4o-mini를 권장합니다.
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 3. API 호출 및 응답 처리
            // 응답 구조가 복잡하므로 Map.class로 받거나 전용 DTO를 정의해야 합니다.
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        } catch (Exception e) {
            return "에러 발생: " + e.getMessage();
        }

        return "응답을 받지 못했습니다.";
    }
}