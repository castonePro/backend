package com.capstone.travelbusan.domain.guide.repository;

import com.capstone.travelbusan.domain.guide.entity.GuideProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuideProductRepository extends JpaRepository<GuideProduct, UUID> {

    // 가이드 본인 상품 전체 (관리용 - 미게시 포함)
    List<GuideProduct> findByGuider_GuideId(UUID guideId);

    // 게시된 상품만 (사용자 탐색용)
    List<GuideProduct> findByIsPublishedTrue();

    // 가이드 본인의 게시된 상품만 (포트폴리오용)
    List<GuideProduct> findByGuider_GuideIdAndIsPublishedTrue(UUID guideId);
}
