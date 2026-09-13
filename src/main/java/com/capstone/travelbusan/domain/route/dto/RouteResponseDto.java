package com.capstone.travelbusan.domain.route.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 자동차 경로 계산 결과 (네이버클라우드 Directions 5 API 응답을 우리 포맷으로 변환).
 */
@Getter
@Builder
public class RouteResponseDto {

    /** 총 이동 거리 (미터) */
    private long distanceMeters;

    /** 총 소요 시간 (초) — 네이버 응답은 밀리초라서 서비스에서 변환함 */
    private long durationSec;

    /** 실제 도로를 따라가는 경로 좌표 목록 (지도에 선으로 그릴 때 사용) */
    private List<LatLng> path;

    @Getter
    @Builder
    public static class LatLng {
        private double lat;
        private double lng;
    }
}
