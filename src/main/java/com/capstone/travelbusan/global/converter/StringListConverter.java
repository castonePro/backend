package com.capstone.travelbusan.global.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * List<String>과 Oracle JSON / VARCHAR2 컬럼 간의 변환기.
 * DB에는 Oracle의 IS JSON 제약조건(ORA-40441 방지)을 완벽히 만족하는 유효한 JSON 배열(["음식", "한식"]) 형태로 저장되고,
 * Java에서는 List<String>으로 변환됩니다.
 * 기존 콤마 구분자("자연,공원") 및 PostgreSQL 배열 포맷({"자연", "공원"}) 데이터도 안전하게 호환 파싱합니다.
 */
@Slf4j
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            // Oracle의 JSON 컬럼 제약조건(ORA-40441 / JZN-00085)을 통과하도록 유효한 JSON 배열 문자열로 직렬화
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            log.error("List<String>을 JSON 문자열로 변환하는 중 오류 발생: {}", attribute, e);
            return "[]";
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return new ArrayList<>();
        }

        String cleaned = dbData.trim();

        // 1. JSON 배열 포맷 파싱 시도 (예: ["음식", "한식"])
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            try {
                return objectMapper.readValue(cleaned, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("JSON 파싱 실패, 수동 파싱 시도: {}", cleaned);
            }
        }

        // 2. PostgreSQL 배열 포맷 대응: {"자연", "공원"}
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        // 3. 콤마 구분자 포맷 대응: "자연,공원"
        return Arrays.stream(cleaned.split(","))
                .map(item -> item.replace("\"", "").replace("'", "").trim())
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
