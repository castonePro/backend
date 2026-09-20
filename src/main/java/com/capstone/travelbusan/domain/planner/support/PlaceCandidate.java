package com.capstone.travelbusan.domain.planner.support;

/**
 * RAG 벡터 검색으로 뽑은 장소 후보 한 건.
 *
 * <p>이 목록이 LLM이 고를 수 있는 전체 선택지이자, 응답 검증의 화이트리스트다.
 */
public record PlaceCandidate(
        long placeId,
        String title,
        String addr1,
        String cat1,
        String cat2,
        String cat3,
        String useTime,
        Double latitude,
        Double longitude,
        String contentChunk
) {
    /** 프롬프트에 넣을 한 줄 표현. 맨 앞 place_id가 모델이 인용해야 할 키다. */
    public String toPromptLine() {
        return String.format(
                "place_id=%d | %s (%s) | 분류: %s %s %s | 운영시간: %s | 좌표: %s, %s | %s",
                placeId,
                blankToDash(title),
                blankToDash(addr1),
                blankToDash(cat1), blankToDash(cat2), blankToDash(cat3),
                blankToDash(useTime),
                latitude != null ? latitude : "?",
                longitude != null ? longitude : "?",
                abbreviate(contentChunk));
    }

    /** cat1~cat3 중 값이 있는 것만 카테고리 태그로 쓴다. patch로 장소를 새로 넣을 때 사용. */
    public java.util.List<String> categoryTags() {
        java.util.List<String> tags = new java.util.ArrayList<>(3);
        for (String c : new String[]{cat1, cat2, cat3}) {
            if (c != null && !c.isBlank() && !tags.contains(c)) tags.add(c);
        }
        return java.util.List.copyOf(tags);
    }

    private static String blankToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private static String abbreviate(String s) {
        if (s == null || s.isBlank()) return "-";
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() <= 200 ? flat : flat.substring(0, 200) + "...";
    }
}
