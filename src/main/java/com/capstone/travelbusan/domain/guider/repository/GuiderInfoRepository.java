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
     * 특정 활동 지역을 포함하는 가이드 목록 조회 (Oracle 호환)
     */
    @Query(value = "SELECT * FROM guider_info WHERE active_regions LIKE '%' || :region || '%'", nativeQuery = true)
    List<GuiderInfo> findByActiveRegion(@Param("region") String region);

    /**
     * 특정 전문 분야를 가진 가이드 목록 조회 (Oracle 호환)
     */
    @Query(value = "SELECT * FROM guider_info WHERE specialties LIKE '%' || :specialty || '%'", nativeQuery = true)
    List<GuiderInfo> findBySpecialty(@Param("specialty") String specialty);
}
