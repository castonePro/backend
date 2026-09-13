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
 * 네이버클라우드 Directions API의 client-secret이 브라우저에 노출되면 안 되기 때문에,
 * 프론트가 구글 지도를 직접 부르듯 네이버를 직접 부르지 않고 이 엔드포인트를 통해 서버가 대신 호출한다.
 */
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /**
     * GET /api/v1/routes/drive?originLat=&originLng=&destLat=&destLng=&waypoints=lat,lng|lat,lng
     */
    @GetMapping("/drive")
    public ResponseEntity<RouteResponseDto> getDrivingRoute(
            @RequestParam double originLat,
            @RequestParam double originLng,
            @RequestParam double destLat,
            @RequestParam double destLng,
            @RequestParam(required = false) String waypoints
    ) {
        List<double[]> parsedWaypoints = new ArrayList<>();
        if (waypoints != null && !waypoints.isBlank()) {
            for (String pair : waypoints.split("\\|")) {
                String[] xy = pair.split(",");
                parsedWaypoints.add(new double[]{Double.parseDouble(xy[0]), Double.parseDouble(xy[1])});
            }
        }

        RouteResponseDto result = routeService.getDrivingRoute(
                originLat, originLng, destLat, destLng, parsedWaypoints);
        return ResponseEntity.ok(result);
    }
}
