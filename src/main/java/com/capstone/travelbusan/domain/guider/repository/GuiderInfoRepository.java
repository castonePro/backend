package com.capstone.travelbusan.domain.guider.repository;

import com.capstone.travelbusan.domain.guider.entity.GuiderInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GuiderInfoRepository extends JpaRepository<GuiderInfo, UUID> {

    /**
     * 특정 활동 지역을 포함하는 가이드 목록 조회
     * PostgreSQL의 배열(@>) 연산자를 활용한 네이티브 쿼리 예시라네.
     */
    @Query(value = "SELECT * FROM guider_info WHERE :region = ANY(active_regions)", nativeQuery = true)
    List<GuiderInfo> findByActiveRegion(@Param("region") String region);

    /**
     * 특정 전문 분야를 가진 가이드 목록 조회
     */
    @Query(value = "SELECT * FROM guider_info WHERE :specialty = ANY(specialties)", nativeQuery = true)
    List<GuiderInfo> findBySpecialty(@Param("specialty") String specialty);
}
