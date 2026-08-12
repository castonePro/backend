package com.capstone.travelbusan.domain.planner.repository;

import com.capstone.travelbusan.domain.planner.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    // 특정 사용자의 모든 일정을 최신순으로 조회
    List<Itinerary> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    //일정 ID와 사용자 UUID가 모두 일치하는 데이터만 조회
    Optional<Itinerary> findByItineraryIdAndUserId(Long itineraryId, UUID userId);
}