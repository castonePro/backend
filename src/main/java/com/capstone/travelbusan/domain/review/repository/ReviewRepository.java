package com.capstone.travelbusan.domain.review.repository;

import com.capstone.travelbusan.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // 가이드 리뷰 목록
    List<Review> findByGuide_IdOrderByCreatedAtDesc(UUID guideId);

    // 가이드 평점 평균
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.guide.id = :guideId")
    Double findAverageRatingByGuideId(@Param("guideId") UUID guideId);

    // 가이드 리뷰 수
    long countByGuide_Id(UUID guideId);

    // 중복 리뷰 방지
    boolean existsByUser_IdAndService_ServiceId(UUID userId, UUID serviceId);
}