package com.capstone.travelbusan.domain.companion.repository;

import com.capstone.travelbusan.domain.companion.entity.CompanionReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompanionReviewRepository extends JpaRepository<CompanionReview, UUID> {

    boolean existsByCompanion_CompanionIdAndReviewer_IdAndReviewee_Id(
            UUID companionId, UUID reviewerId, UUID revieweeId);

    // 특정 동행에서 revieweeId가 받은 리뷰들 (공개 여부는 서비스 레이어에서 쌍 매칭으로 판정)
    List<CompanionReview> findByCompanion_CompanionIdAndReviewee_Id(UUID companionId, UUID revieweeId);

    // 프로필 요약(평균 평점)용 — 내가 받은 전체 리뷰
    List<CompanionReview> findByReviewee_Id(UUID revieweeId);
}
