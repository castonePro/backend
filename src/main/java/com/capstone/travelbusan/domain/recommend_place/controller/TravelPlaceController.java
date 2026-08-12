package com.capstone.travelbusan.domain.recommend_place.controller;

import com.capstone.travelbusan.domain.recommend_place.dto.TravelPlaceDetailDto;
import com.capstone.travelbusan.domain.recommend_place.entity.TravelPlace;
import com.capstone.travelbusan.domain.recommend_place.service.TravelPlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class TravelPlaceController {

    private final TravelPlaceService travelPlaceService;

    // 카테고리별 추천 여행지
    // category: 음식, 인문(문화/예술/역사), 숙박, 쇼핑, 레포츠, 자연, 추천코스
    @GetMapping("/recommend")
    public ResponseEntity<List<TravelPlace>> getRecommend(
            @RequestParam(defaultValue = "인문(문화/예술/역사)") String category) {
        return ResponseEntity.ok(travelPlaceService.getRecommendByCategory(category));
    }

    // 인기 장소 TOP 4
    @GetMapping("/popular")
    public ResponseEntity<List<TravelPlace>> getPopular() {
        return ResponseEntity.ok(travelPlaceService.getPopularPlaces());
    }
    // 장소 상세 조회
    @GetMapping("/{placeId}")
    public ResponseEntity<TravelPlaceDetailDto> getPlaceDetail(@PathVariable Integer placeId){
            return ResponseEntity.ok(travelPlaceService.getPlaceDetail(placeId));
    }
}