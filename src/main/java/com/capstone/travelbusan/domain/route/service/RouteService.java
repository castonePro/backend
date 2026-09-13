package com.capstone.travelbusan.domain.route.service;

import com.capstone.travelbusan.domain.route.dto.RouteResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 네이버클라우드플랫폼(NCP) Maps Directions 5 API로 자동차 경로(거리·소요시간·경로좌표)를 계산한다.
 *
 * 원래는 구글 Routes API를 썼는데, 한국 내 경로는 한국 정부의 정밀지도 데이터 반출 규제 때문에
 * 구글이 아직 응답을 못 내려줘서(HTTP 200이지만 routes가 빈 배열) 네이버로 교체함 (2026-09).
 *
 * 자동차 경로만 지원한다 — 네이버 Directions API 자체가 도보 모드를 제공하지 않기 때문에,
 * 도보 이동은 프론트엔드에서 직선거리(하버사인 공식)로 추정치만 보여준다.
 *
 * client-secret은 노출되면 안 되는 키라서 반드시 백엔드에서만 호출한다 (프론트에서 직접 호출 금지).
 */
@Service
public class RouteService {

    @Value("${naver.maps.client-id}")
    private String clientId;

    @Value("${naver.maps.client-secret}")
    private String clientSecret;

    private static final String ENDPOINT = "https://maps.apigw.ntruss.com/map-direction/v1/driving";

    // Directions 5 API 제한: 경유지(waypoints) 최대 5개
    private static final int MAX_WAYPOINTS = 5;

    private final RestTemplate restTemplate = new RestTemplate();

    public RouteResponseDto getDrivingRoute(double originLat, double originLng,
                                             double destLat, double destLng,
                                             List<double[]> waypoints) {
        // 네이버는 "경도,위도(lng,lat)" 순서로 받는다 (위도경도 순 아님, 헷갈리기 쉬운 부분)
        String start = originLng + "," + originLat;
        String goal = destLng + "," + destLat;

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ENDPOINT)
                .queryParam("start", start)
                .queryParam("goal", goal)
                .queryParam("option", "trafast");

        if (waypoints != null && !waypoints.isEmpty()) {
            List<double[]> limited = waypoints.size() > MAX_WAYPOINTS
                    ? waypoints.subList(0, MAX_WAYPOINTS)
                    : waypoints;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < limited.size(); i++) {
                double[] p = limited.get(i); // p[0]=lat, p[1]=lng (컨트롤러에서 이 순서로 넘겨줌)
                if (i > 0) sb.append("|");
                sb.append(p[1]).append(",").append(p[0]);
            }
            builder.queryParam("waypoints", sb.toString());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-ncp-apigw-api-key-id", clientId);
        headers.set("x-ncp-apigw-api-key", clientSecret);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, entity, Map.class
            ).getBody();

            if (response == null) {
                throw new IllegalStateException("네이버 길찾기 API로부터 응답을 받지 못했습니다.");
            }

            Number code = (Number) response.get("code");
            if (code == null || code.intValue() != 0) {
                throw new IllegalStateException("네이버 길찾기 실패: " + response.get("message"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> route = (Map<String, Object>) response.get("route");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) route.get("trafast");
            if (results == null || results.isEmpty()) {
                throw new IllegalStateException("네이버 길찾기 결과가 비어 있습니다.");
            }

            Map<String, Object> first = results.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) first.get("summary");
            long distanceMeters = ((Number) summary.get("distance")).longValue();
            long durationMs = ((Number) summary.get("duration")).longValue();

            @SuppressWarnings("unchecked")
            List<List<Number>> rawPath = (List<List<Number>>) first.get("path");
            List<RouteResponseDto.LatLng> path = new ArrayList<>();
            if (rawPath != null) {
                for (List<Number> point : rawPath) {
                    // 네이버 path 좌표도 [경도, 위도] 순서
                    path.add(RouteResponseDto.LatLng.builder()
                            .lng(point.get(0).doubleValue())
                            .lat(point.get(1).doubleValue())
                            .build());
                }
            }

            return RouteResponseDto.builder()
                    .distanceMeters(distanceMeters)
                    .durationSec(durationMs / 1000)
                    .path(path)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("네이버 길찾기 API 호출 실패: " + e.getMessage(), e);
        }
    }
}
