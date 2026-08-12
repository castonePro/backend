package com.capstone.travelbusan.aitest;

import com.capstone.travelbusan.domain.ai.service.AiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
class RagPipelineTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    @DisplayName("RAG 전체 프로세스 테스트: 질문 -> 임베딩 -> DB 검색 -> GPT 답변")
    void fullRagProcessTest() {
        // 1. 사용자의 질문 설정
        String userQuestion = "양식 종류로 음식점 추천해줘 광안리 근처로.";
        System.out.println("💬 [사용자 질문]: " + userQuestion);

        // 2. 파이썬 서버를 통해 질문을 벡터(Embedding)로 변환
        // e5 모델 특성상 검색 쿼리에는 'query: ' 접두사를 붙이는 것이 정확도가 높습니다.
        String vectorString = getEmbeddingFromPython("query: " + userQuestion);
        System.out.println("🧬 [임베딩]: 질문의 벡터화 완료");

        // 3. PostgreSQL(pgvector)를 이용한 유사도 검색
        // <=> 연산자는 코사인 거리(Cosine Distance)를 계산합니다.
        String sql = """
                SELECT p.title, p.addr1, v.content_chunk 
                FROM travel_places p 
                JOIN travel_vectors v ON p.place_id = v.place_id 
                ORDER BY v.embedding <=> ?::vector 
                LIMIT 3
                """;

        List<Map<String, Object>> searchResults = jdbcTemplate.queryForList(sql, vectorString);
        System.out.println("🔍 [DB 검색]: 가장 유사한 장소 " + searchResults.size() + "건 발견");

        // 4. 검색된 장소 정보를 문맥(Context)으로 조립
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < searchResults.size(); i++) {
            Map<String, Object> row = searchResults.get(i);
            context.append(String.format("[%d] 장소명: %s\n주소: %s\n상세정보: %s\n\n",
                    i + 1, row.get("title"), row.get("addr1"), row.get("content_chunk")));
        }

        // 5. GPT에게 던질 최종 프롬프트 구성 (System Prompt + Context + Question)
        String finalPrompt = String.format("""
                너는 부산 여행 전문가 AI 가이드야. 아래 제공된 [관광지 데이터]만을 근거로 사용자의 질문에 친절하게 답변해줘.
                만약 데이터에 답이 없다면 지어내지 말고 "정보를 찾을 수 없다"고 대답해.
                
                [관광지 데이터]
                %s
                
                [사용자 질문]
                %s
                
                답변:
                """, context.toString(), userQuestion);

        // 6. AiService(GPT API) 호출
        System.out.println("🤖 [GPT 호출]: 답변 생성 중...");
        String response = aiService.getChatResponse(finalPrompt);

        // 7. 결과 출력
        System.out.println("\n================ [ AI 가이드의 추천 ] ================\n");
        System.out.println(response);
        System.out.println("\n====================================================");
    }

    /**
     * FastAPI 서버(8000포트)와 통신하여 벡터 배열을 문자열로 받아옵니다.
     */
    private String getEmbeddingFromPython(String text) {
        String url = "http://localhost:8000/embed";
        Map<String, String> request = Map.of("text", text);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            if (response != null && response.containsKey("embedding")) {
                List<Double> vector = (List<Double>) response.get("embedding");
                return vector.toString(); // "[0.1, 0.2, ...]" 형태의 문자열 반환
            }
        } catch (Exception e) {
            throw new RuntimeException("파이썬 임베딩 서버 호출 실패! 서버가 켜져 있는지 확인하세요.", e);
        }
        return null;
    }
}