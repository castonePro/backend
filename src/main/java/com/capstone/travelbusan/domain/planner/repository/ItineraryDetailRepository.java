package com.capstone.travelbusan.domain.planner.repository;

import com.capstone.travelbusan.domain.planner.entity.ItineraryDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ItineraryDetailRepository extends JpaRepository<ItineraryDetail, Long> {

    /**
     * 특정 일정(itineraryId)에 속한 상세 코스들을 정렬하여 조회합니다.
     */
    List<ItineraryDetail> findByItinerary_ItineraryIdOrderByDayNumberAscSortOrderAsc(Long itineraryId);
}