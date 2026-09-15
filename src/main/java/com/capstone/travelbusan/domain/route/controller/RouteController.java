package com.capstone.travelbusan.domain.route.controller;

import com.capstone.travelbusan.domain.route.dto.RouteResponseDto;
import com.capstone.travelbusan.domain.route.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 일정 코스 자동차 경로 계산 API.
 * 네이버클라우드 Directions API의 인증키는 서버에서만 사용해야 하므로,
 * 프론트는 이 엔드포인트를 통해 서버가 대신 호출한 결과를 받는다.
 */
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    // 네이버 Directions 5 API 제한과 동일 (RouteService.MAX_WAYPOINTS 참고)
    private static final int MAX_WAYPOINTS = 5;

    /**
     * 자동차 경로 조회.
     * GET /api/v1/routes/drive?originLat=&originLng=&destLat=&destLng=&waypoints=lat,lng|lat,lng
     *
     * @param waypoints 경유지 좌표 목록. "위도,경도" 쌍을 "|"로 구분한 문자열. 형식이 올바르지
     *                  않으면 400 Bad Request로 응답한다.
     */
    @GetMapping("/drive")
    public ResponseEntity<RouteResponseDto> getDrivingRoute(
            @RequestParam double originLat,
            @RequestParam double originLng,
            @RequestParam double destLat,
            @RequestParam double destLng,
            @RequestParam(required = false) String waypoints
    ) {
        validateCoordinate(originLat, originLng, "출발지");
        validateCoordinate(destLat, destLng, "도착지");

        List<double[]> parsedWaypoints = parseWaypoints(waypoints);

        RouteResponseDto result = routeService.getDrivingRoute(
                originLat, originLng, destLat, destLng, parsedWaypoints);
        return ResponseEntity.ok(result);
    }

    /**
     * "위도,경도|위도,경도" 형식의 경유지 문자열을 파싱한다.
     * 값이 없으면 빈 목록을 반환하고, 형식이 잘못된 경우 IllegalArgumentException을 던져
     * GlobalExceptionHandler가 400 Bad Request로 응답하게 한다.
     */
    private List<double[]> parseWaypoints(String waypoints) {
        List<double[]> result = new ArrayList<>();
        if (waypoints == null || waypoints.isBlank()) {
            return result;
        }

        for (String pair : waypoints.split("\\|")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "경유지 좌표 형식이 올바르지 않습니다: \"" + pair + "\" (예: 35.1587,129.1601)");
            }
            try {
                double lat = Double.parseDouble(parts[0].trim());
                double lng = Double.parseDouble(parts[1].trim());
                validateCoordinate(lat, lng, "경유지");
                result.add(new double[]{lat, lng});
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "경유지 좌표 숫자 형식이 올바르지 않습니다: \"" + pair + "\"", e);
            }
        }

        if (result.size() > MAX_WAYPOINTS) {
            throw new IllegalArgumentException(
                    "경유지는 최대 " + MAX_WAYPOINTS + "개까지 지원합니다. (요청: " + result.size() + "개)");
        }
        return result;
    }

    /**
     * 위도는 -90~90, 경도는 -180~180 범위를 벗어나면 명백히 잘못된 좌표이므로 요청 단계에서 걸러낸다.
     * Double.parseDouble("NaN")/("Infinity")은 예외 없이 통과되므로 범위 비교만으로는 걸러지지 않는데
     * (NaN과의 비교는 항상 false를 반환하기 때문), 그래서 범위 체크 전에 NaN/무한대 여부를 먼저 확인한다.
     */
    private void validateCoordinate(double lat, double lng, String label) {
        if (Double.isNaN(lat) || Double.isNaN(lng) || Double.isInfinite(lat) || Double.isInfinite(lng)
                || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new IllegalArgumentException(
                    label + " 좌표 값이 올바르지 않습니다: 위도=" + lat + ", 경도=" + lng);
        }
    }
}
