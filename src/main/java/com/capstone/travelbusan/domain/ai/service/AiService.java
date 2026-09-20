package com.capstone.travelbusan.domain.ai.service;

import com.capstone.travelbusan.domain.ai.dto.AiChatOptions;
import com.capstone.travelbusan.domain.ai.dto.AiChatResult;
import com.capstone.travelbusan.domain.ai.dto.AiJsonResult;
import com.capstone.travelbusan.domain.ai.dto.AiMessage;
import com.capstone.travelbusan.domain.ai.exception.AiApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat / Embedding 호출 계층.
 *
 * <p>멀티턴 대화를 붙이기 위해 다음을 지원하도록 확장했다:
 * <ul>
 *   <li>메시지 배열 입력 — system 프롬프트 분리와 이전 턴 히스토리 전달</li>
 *   <li>Structured Outputs(json_schema, strict) — 스키마 위반 응답 원천 차단</li>
 *   <li>토큰 사용량 반환 — 세션당 비용 상한을 걸기 위한 전제</li>
 *   <li>명시적 타임아웃 — 기본 RestTemplate은 타임아웃이 무한이라 스레드가 묶인다</li>
 * </ul>
 *
 * <p>기존 {@link #getChatResponse(String)}은 호환을 위해 남겨두고 내부적으로
 * {@link #chat(List, AiChatOptions)}에 위임한다.
 */
@Slf4j
@Service
public class AiService {

    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String EMBEDDING_URL = "https://api.openai.com/v1/embeddings";
    private static final String EMBEDDING_MODEL = "text-embedding-3-small";

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 120_000;

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public AiService(@Value("${spring.ai.openai.api-key}") String apiKey,
                     ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    // ─────────────────────────── Chat ───────────────────────────

    /**
     * 멀티턴 대화 호출. messages 순서가 그대로 모델에 전달된다.
     *
     * @param messages system/user/assistant 메시지 목록 (비어 있으면 예외)
     * @param options  모델·토큰 상한·응답 스키마. null이면 기본값
     */
    public AiChatResult chat(List<AiMessage> messages, AiChatOptions options) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("chat(): messages가 비어 있습니다.");
        }
        AiChatOptions opts = (options != null) ? options : AiChatOptions.defaults();
        String model = (opts.model() != null) ? opts.model() : AiChatOptions.DEFAULT_MODEL;

        List<Map<String, Object>> payloadMessages = new ArrayList<>(messages.size());
        for (AiMessage m : messages) {
            payloadMessages.add(Map.of("role", m.role(), "content", m.content()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", payloadMessages);
        if (opts.temperature() != null) {
            body.put("temperature", opts.temperature());
        }
        if (opts.maxTokens() != null) {
            body.put("max_tokens", opts.maxTokens());
        }
        if (opts.responseSchema() != null) {
            body.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", opts.responseSchema().name(),
                            "strict", true,
                            "schema", opts.responseSchema().schema())));
        }

        JsonNode root = post(CHAT_URL, body, "Chat");

        JsonNode choice = root.path("choices").path(0);
        if (choice.isMissingNode()) {
            throw new AiApiException("OpenAI 응답에 choices가 없습니다: " + abbreviate(root.toString()));
        }

        JsonNode message = choice.path("message");
        if (message.path("refusal").isTextual()) {
            throw new AiApiException("모델이 응답을 거부했습니다: " + message.path("refusal").asText());
        }

        String content = message.path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new AiApiException("OpenAI 응답 content가 비어 있습니다.");
        }

        String finishReason = choice.path("finish_reason").asText(null);
        JsonNode usage = root.path("usage");
        AiChatResult result = new AiChatResult(
                content,
                usage.path("prompt_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0),
                finishReason);

        if (result.truncated()) {
            log.warn("OpenAI 응답이 max_tokens({})에서 잘렸습니다. JSON이 불완전할 수 있습니다.", opts.maxTokens());
        }
        log.info("OpenAI chat 완료: model={}, in={}, out={}, finish={}",
                model, result.promptTokens(), result.completionTokens(), finishReason);

        return result;
    }

    /**
     * Structured Outputs 응답을 바로 DTO로 역직렬화한다.
     * options.responseSchema()가 설정되어 있어야 의미가 있다.
     */
    public <T> T chatAsJson(List<AiMessage> messages, AiChatOptions options, Class<T> type) {
        return chatAsJsonWithUsage(messages, options, type).value();
    }

    /**
     * {@link #chatAsJson}과 같지만 토큰 사용량까지 함께 돌려준다.
     * 세션당 누적 토큰 상한을 거는 호출부에서 쓴다.
     */
    public <T> AiJsonResult<T> chatAsJsonWithUsage(List<AiMessage> messages, AiChatOptions options, Class<T> type) {
        AiChatResult result = chat(messages, options);
        try {
            return new AiJsonResult<>(objectMapper.readValue(result.content(), type), result);
        } catch (Exception e) {
            throw new AiApiException(
                    "구조화 응답 파싱 실패(" + type.getSimpleName() + "): " + abbreviate(result.content()), e);
        }
    }

    /**
     * 단발 user 메시지 호출. 기존 호출부 호환용.
     *
     * @deprecated 멀티턴·스키마 강제가 필요하면 {@link #chat(List, AiChatOptions)}를 쓴다.
     */
    @Deprecated
    public String getChatResponse(String prompt) {
        return chat(List.of(AiMessage.user(prompt)), AiChatOptions.defaults()).content();
    }

    // ───────────────────────── Embedding ─────────────────────────

    /**
     * 텍스트를 768차원 벡터 문자열로 변환한다.
     * Oracle VECTOR / pgvector에 바로 바인딩 가능한 {@code [0.123, -0.456, ...]} 형태.
     */
    public String getEmbedding(String text) {
        return getEmbedding(text, 768);
    }

    public String getEmbedding(String text, int dimensions) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("getEmbedding(): text가 비어 있습니다.");
        }

        Map<String, Object> body = Map.of(
                "model", EMBEDDING_MODEL,
                "input", text,
                "dimensions", dimensions);

        JsonNode root = post(EMBEDDING_URL, body, "Embedding");
        JsonNode embedding = root.path("data").path(0).path("embedding");
        if (!embedding.isArray() || embedding.isEmpty()) {
            throw new AiApiException("OpenAI 임베딩 응답이 비어 있습니다: " + abbreviate(root.toString()));
        }

        StringBuilder sb = new StringBuilder(embedding.size() * 12).append('[');
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(embedding.get(i).asDouble());
        }
        return sb.append(']').toString();
    }

    // ────────────────────────── 내부 공통 ──────────────────────────

    private JsonNode post(String url, Map<String, Object> body, String label) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String raw;
        try {
            raw = restTemplate.postForObject(url, entity, String.class);
        } catch (RestClientException e) {
            throw new AiApiException("OpenAI " + label + " API 호출 실패: " + e.getMessage(), e);
        }

        if (raw == null || raw.isBlank()) {
            throw new AiApiException("OpenAI " + label + " 응답이 비어 있습니다.");
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new AiApiException("OpenAI " + label + " 응답 파싱 실패: " + abbreviate(raw), e);
        }
    }

    private static String abbreviate(String s) {
        if (s == null) return "null";
        return (s.length() <= 500) ? s : s.substring(0, 500) + "...(생략)";
    }
}
