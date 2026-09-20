package com.capstone.travelbusan.domain.planner;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAI Structured Outputs strict 모드 제약 검사기.
 *
 * <p>strict 모드는 object마다 additionalProperties:false 를 요구하고, 모든 property가
 * required에 들어가야 한다. 선택적 필드는 required에서 빼는 게 아니라 타입에 "null"을
 * 더해서 표현한다. 하나라도 어기면 OpenAI가 400을 돌려주는데, 스키마를 손볼 때
 * 놓치기 쉬워서 테스트로 고정한다.
 */
final class SchemaAssertions {

    private SchemaAssertions() {}

    @SuppressWarnings("unchecked")
    static void assertStrict(Map<String, Object> node, String path) {
        Object type = node.get("type");

        if ("array".equals(type)) {
            Object items = node.get("items");
            assertThat(items).as("%s: array에 items가 있어야 한다", path).isInstanceOf(Map.class);
            assertStrict((Map<String, Object>) items, path + "[]");
            return;
        }
        if (!"object".equals(type)) {
            // 스칼라거나 nullable 타입(["integer","null"]). 더 내려갈 것이 없다.
            return;
        }

        assertThat(node.get("additionalProperties"))
                .as("%s: additionalProperties가 false여야 한다", path)
                .isEqualTo(false);

        Map<String, Object> properties = (Map<String, Object>) node.get("properties");
        List<String> required = (List<String>) node.get("required");

        assertThat(properties).as("%s: properties가 있어야 한다", path).isNotNull();
        assertThat(required)
                .as("%s: required가 properties 키 전체와 일치해야 한다", path)
                .containsExactlyInAnyOrderElementsOf(properties.keySet());

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            assertStrict((Map<String, Object>) entry.getValue(), path + "." + entry.getKey());
        }
    }
}
