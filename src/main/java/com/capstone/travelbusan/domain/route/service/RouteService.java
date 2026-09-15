package com.capstone.travelbusan.domain.route.service;

import com.capstone.travelbusan.domain.route.dto.RouteResponseDto;
import com.capstone.travelbusan.domain.route.exception.RouteApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 네이버클라우드플랫폼(NCP) Maps Directions 5 API를 호출해 자동차 경로(거리·소요시간·경로좌표)를 계산한다.
 *
 * 자동차 경로만 지원한다. 네이버 Directions API는 도보 모드를 제공하지 않으므로,
 * 도보 이동 거리·시간은 프론트엔드에서 직선거리(하버사인 공식) 기반으로 추정한다.
 *
 * client-secret은 브라우저에 노출되면 안 되는 키이므로 이 서비스는 반드시 서버에서만 호출해야 하며,
 * 프론트엔드가 네이버 API를 직접 호출하도록 이 값을 전달해서는 안 된다.
 */
@Slf4j
@Service
public class RouteService {

    @Value("${naver.maps.client-id}")
    private String clientId;

    @Value("${naver.maps.client-secret}")
    private String clientSecret;

    private static final String ENDPOINT = "https://maps.apigw.ntruss.com/map-direction/v1/driving";

    /** Directions 5 API 제한: 경유지(waypoints)는 최대 5개까지만 허용된다. */
    private static final int MAX_WAYPOINTS = 5;

    /** application-naver.yaml.example에 있는 안내용 placeholder 값의 일부. 실수로 그대로 두고 실행한 경우를 잡아낸다. */
    private static final String PLACEHOLDER_MARKER = "여기에_";

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        // 네이버 서버가 응답이 없을 때 요청이 무한정 걸려있지 않도록 타임아웃을 명시적으로 설정한다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    public RouteResponseDto getDrivingRoute(double originLat, double originLng,
                                             double destLat, double destLng,
                                             List<double[]> waypoints) {
        String requestUrl = buildRequestUrl(originLat, originLng, destLat, destLng, waypoints);
        Map<String, Object> response = callNaverDirectionsApi(requestUrl);
        return parseResponse(response);
    }

    private String buildRequestUrl(double originLat, double originLng,
                                    double destLat, double destLng,
                                    List<double[]> waypoints) {
        // 네이버는 좌표를 "경도,위도(lng,lat)" 순서로 받는다 (위경도 순서가 아니므로 주의)
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
                double[] point = limited.get(i); // point[0]=lat, point[1]=lng (컨트롤러가 이 순서로 전달)
                if (i > 0) {
                    sb.append("|");
                }
                sb.append(point[1]).append(",").append(point[0]);
            }
            builder.queryParam("waypoints", sb.toString());
        }

        return builder.toUriString();
    }

    /**
     * client-id/client-secret이 비어 있거나 example 파일의 안내 문구가 그대로 남아 있으면
     * 네이버 서버까지 요청을 보내지 않고 여기서 바로 원인이 분명한 오류로 실패시킨다.
     * (실제 키를 안 채워 넣고 실행해서 "인증 실패"로만 보이는 상황을 방지하기 위함)
     */
    private void ensureCredentialsConfigured() {
        if (isBlankOrPlaceholder(clientId) || isBlankOrPlaceholder(clientSecret)) {
            throw new RouteApiException(
                    "네이버클라우드 Maps API 키가 설정되지 않았습니다. "
                            + "application-naver.yaml에 실제 Client ID/Secret 값을 입력했는지 확인해 주세요.");
        }
    }

    private boolean isBlankOrPlaceholder(String value) {
        return value == null || value.isBlank() || value.contains(PLACEHOLDER_MARKER);
    }

    /**
     * 네이버 Directions API를 호출한다.
     * 실패 원인(인증/요청/서버/네트워크/그 외 예상치 못한 오류)을 구분해 각각 명확한 메시지의
     * RouteApiException으로 변환한다 — 원인과 무관하게 뭉뚱그려 500을 내려주지 않기 위함이다.
     */
    private Map<String, Object> callNaverDirectionsApi(String requestUrl) {
        ensureCredentialsConfigured();

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-ncp-apigw-api-key-id", clientId);
        headers.set("x-ncp-apigw-api-key", clientSecret);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, Object> response;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.exchange(
                    requestUrl, HttpMethod.GET, entity, Map.class
            ).getBody();
            response = body;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.error("네이버 길찾기 API 인증 실패 (status={}, body={})", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RouteApiException(
                    "네이버 길찾기 API 인증에 실패했습니다. Client ID/Secret 및 API 구독 상태를 확인해 주세요.", e);
        } catch (HttpClientErrorException e) {
            log.error("네이버 길찾기 API 요청 거부 (status={}, body={})", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RouteApiException(
                    "네이버 길찾기 API 요청이 거부되었습니다 (status=" + e.getStatusCode().value() + ").", e);
        } catch (HttpServerErrorException e) {
            log.error("네이버 길찾기 API 서버 오류 (status={}, body={})", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RouteApiException("네이버 길찾기 API 서버에 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", e);
        } catch (ResourceAccessException e) {
            log.error("네이버 길찾기 API 연결 실패(타임아웃 포함): {}", e.getMessage());
            throw new RouteApiException(
                    "네이버 길찾기 API 서버에 연결할 수 없습니다. 네트워크 상태 또는 요청 시간 초과 여부를 확인해 주세요.", e);
        } catch (RestClientException e) {
            log.error("네이버 길찾기 API 호출 중 알 수 없는 오류: {}", e.getMessage());
            throw new RouteApiException("네이버 길찾기 API 호출 중 오류가 발생했습니다.", e);
        } catch (RuntimeException e) {
            // 위에서 잡지 못한, 정말 예상치 못한 오류까지 안전망으로 잡아서
            // 원인 불명의 500 대신 일관되게 502(RouteApiException)로 응답한다.
            log.error("네이버 길찾기 API 호출 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new RouteApiException("네이버 길찾기 API 호출 중 예상치 못한 오류가 발생했습니다.", e);
        }

        if (response == null) {
            throw new RouteApiException("네이버 길찾기 API 응답 본문이 비어 있습니다.");
        }
        return response;
    }

    /**
     * 네이버 응답 JSON을 우리 응답 포맷으로 변환한다.
     * 응답 구조가 예상과 다른 경우(필드 누락 등) 각 단계에서 원인이 드러나는 메시지로 실패시킨다.
     */
    private RouteResponseDto parseResponse(Map<String, Object> response) {
        Number code = asNumber(response.get("code"));
        if (code == null || code.intValue() != 0) {
            Object message = response.get("message");
            log.error("네이버 길찾기 실패 응답: code={}, message={}", code, message);
            throw new RouteApiException("네이버 길찾기 요청이 실패했습니다: " + message);
        }

        Object routeObj = response.get("route");
        if (!(routeObj instanceof Map)) {
            throw new RouteApiException("네이버 길찾기 응답 형식이 올바르지 않습니다 (route 필드 없음).");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> route = (Map<String, Object>) routeObj;

        Object trafastObj = route.get("trafast");
        if (!(trafastObj instanceof List) || ((List<?>) trafastObj).isEmpty()) {
            throw new RouteApiException("네이버 길찾기 결과가 비어 있습니다. 출발지/도착지 좌표를 확인해 주세요.");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) trafastObj;
        Map<String, Object> first = results.get(0);
        if (first == null) {
            throw new RouteApiException("네이버 길찾기 응답 형식이 올바르지 않습니다 (경로 항목이 비어 있음).");
        }

        Object summaryObj = first.get("summary");
        if (!(summaryObj instanceof Map)) {
            throw new RouteApiException("네이버 길찾기 응답 형식이 올바르지 않습니다 (summary 필드 없음).");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) summaryObj;

        Number distance = asNumber(summary.get("distance"));
        Number duration = asNumber(summary.get("duration"));
        if (distance == null || duration == null) {
            throw new RouteApiException("네이버 길찾기 응답에 거리/소요시간 정보가 없습니다.");
        }
        if (distance.longValue() < 0 || duration.longValue() < 0) {
            throw new RouteApiException("네이버 길찾기 응답의 거리/소요시간 값이 올바르지 않습니다.");
        }

        List<RouteResponseDto.LatLng> path = parsePath(first.get("path"));

        return RouteResponseDto.builder()
                .distanceMeters(distance.longValue())
                .durationSec(duration.longValue() / 1000) // 네이버는 밀리초 단위로 응답함
                .path(path)
                .build();
    }

    private List<RouteResponseDto.LatLng> parsePath(Object rawPathObj) {
        List<RouteResponseDto.LatLng> path = new ArrayList<>();
        if (!(rawPathObj instanceof List)) {
            return path;
        }
        for (Object rawPoint : (List<?>) rawPathObj) {
            if (!(rawPoint instanceof List) || ((List<?>) rawPoint).size() < 2) {
                continue; // 형식이 다른 좌표는 건너뛴다 (지도 표시가 약간 끊기더라도 전체 요청은 실패시키지 않음)
            }
            List<?> point = (List<?>) rawPoint;
            Object lngRaw = point.get(0);
            Object latRaw = point.get(1);
            if (!(lngRaw instanceof Number) || !(latRaw instanceof Number)) {
                continue;
            }
            // 네이버 path 좌표도 [경도, 위도] 순서
            path.add(RouteResponseDto.LatLng.builder()
                    .lng(((Number) lngRaw).doubleValue())
                    .lat(((Number) latRaw).doubleValue())
                    .build());
        }
        return path;
    }

    private Number asNumber(Object value) {
        return (value instanceof Number) ? (Number) value : null;
    }
}
