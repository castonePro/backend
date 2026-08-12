package com.capstone.travelbusan.domain.recommend_place.repository;

import com.capstone.travelbusan.domain.recommend_place.entity.TravelPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelPlaceRepository extends JpaRepository<TravelPlace, Integer> {

    // place_name으로 단건 조회 (플래너 저장 시 place_id 매핑용)
    Optional<TravelPlace> findByTitle(String title);

    // 카테고리별 이미지 있는 장소 랜덤 N개
    @Query(value = """
            SELECT * FROM travel_places
            WHERE cat1 = :category
            AND first_image IS NOT NULL
            AND first_image != ''
            ORDER BY RANDOM()
            LIMIT :limit
            """, nativeQuery = true)
    List<TravelPlace> findRandomByCategory(
            @Param("category") String category,
            @Param("limit") int limit
    );

    // itinerary_details에서 많이 사용된 place_id 기준 TOP N
    @Query(value = """
            SELECT tp.* FROM travel_places tp
            INNER JOIN (
                SELECT place_id, COUNT(*) as cnt
                FROM itinerary_details
                WHERE place_id IS NOT NULL
                GROUP BY place_id
                ORDER BY cnt DESC
                LIMIT :limit
            ) popular ON tp.place_id = popular.place_id
            WHERE tp.first_image IS NOT NULL
            AND tp.first_image != ''
            ORDER BY popular.cnt DESC
            """, nativeQuery = true)
    List<TravelPlace> findPopularPlaces(@Param("limit") int limit);
}