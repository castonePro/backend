package com.capstone.travelbusan.domain.bidapplication.repository;
import com.capstone.travelbusan.domain.bidapplication.entity.BidApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidApplicationRepository extends JpaRepository<BidApplication, UUID> {

    // 특정 입찰에 참여한 가이드 목록
    List<BidApplication> findByUserBid_BidId(UUID bidId);

    // 중복 참여 체크
    boolean existsByUserBid_BidIdAndGuide_Id(UUID bidId, UUID guideId);

    // 가이드 본인의 참여 목록
    List<BidApplication> findByGuide_Id(UUID guideId);

    // 특정 입찰 + 특정 가이드 조회 (취소용)
    Optional<BidApplication> findByUserBid_BidIdAndGuide_Id(UUID bidId, UUID guideId);
}