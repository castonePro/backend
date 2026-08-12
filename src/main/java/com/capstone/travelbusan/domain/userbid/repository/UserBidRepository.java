package com.capstone.travelbusan.domain.userbid.repository;

import com.capstone.travelbusan.domain.userbid.entity.UserBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserBidRepository extends JpaRepository<UserBid, UUID> {

    // 가이드 입찰 현황 조회 (모든 사용자 요청 목록)
    List<UserBid> findAllByOrderByCreatedAtDesc();

    // 사용자 본인 요청 목록
    List<UserBid> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    // 중복 요청 방지
    boolean existsByItinerary_ItineraryIdAndUser_Id(Long itineraryId, UUID userId);
}