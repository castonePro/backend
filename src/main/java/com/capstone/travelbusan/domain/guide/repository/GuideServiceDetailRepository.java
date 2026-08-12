package com.capstone.travelbusan.domain.guide.repository;

import com.capstone.travelbusan.domain.guide.entity.GuideServiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuideServiceDetailRepository extends JpaRepository<GuideServiceDetail, UUID> {

    // 상품의 스케줄 전체 조회 (순서 정렬)
    List<GuideServiceDetail> findByService_ServiceIdOrderBySequenceOrderAsc(UUID serviceId);

    // 상품 수정 시 기존 스케줄 전체 삭제
    void deleteByService_ServiceId(UUID serviceId);
}
