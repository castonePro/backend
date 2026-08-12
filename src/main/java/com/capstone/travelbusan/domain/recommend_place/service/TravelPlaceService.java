package com.capstone.travelbusan.domain.recommend_place.service;

import com.capstone.travelbusan.domain.recommend_place.dto.TravelPlaceDetailDto;
import com.capstone.travelbusan.domain.recommend_place.entity.TravelPlace;
import com.capstone.travelbusan.domain.recommend_place.repository.TravelPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TravelPlaceService {

    private final TravelPlaceRepository travelPlaceRepository;
    private final JdbcTemplate jdbcTemplate;

    // 카테고리별 랜덤 장소
    public List<TravelPlace> getRecommendByCategory(String category) {
        return travelPlaceRepository.findRandomByCategory(category, 5);
    }

    // 인기 장소 TOP 4
    public List<TravelPlace> getPopularPlaces() {
        return travelPlaceRepository.findPopularPlaces(4);
    }

    // 장소 상세 조회 (travel_descriptions JOIN)
    public TravelPlaceDetailDto getPlaceDetail(Integer placeId) {
        String sql = """
                SELECT tp.place_id, tp.title, tp.addr1, tp.first_image, tp.first_image2,
                       tp.cat1, tp.cat2, tp.cat3,
                       td.homepage, td.overview
                FROM travel_places tp
                LEFT JOIN travel_descriptions td ON tp.place_id = td.place_id
                WHERE tp.place_id = ?
                """;

        Map<String, Object> row = jdbcTemplate.queryForMap(sql, placeId);

        return TravelPlaceDetailDto.builder()
                .placeId(((Number) row.get("place_id")).intValue())
                .title((String) row.get("title"))
                .addr1((String) row.get("addr1"))
                .firstImage((String) row.get("first_image"))
                .firstImage2((String) row.get("first_image2"))
                .cat1(row.get("cat1") != null ? (String) row.get("cat1") : "")
                .cat2(row.get("cat2") != null ? (String) row.get("cat2") : "")
                .cat3(row.get("cat3") != null ? (String) row.get("cat3") : "")
                .homepage(row.get("homepage") != null ? (String) row.get("homepage") : "")
                .overview(row.get("overview") != null ? (String) row.get("overview") : "상세 설명이 없습니다.") // null 처리
                .build();
    }
}