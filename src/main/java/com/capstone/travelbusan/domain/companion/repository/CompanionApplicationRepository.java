package com.capstone.travelbusan.domain.companion.repository;

import com.capstone.travelbusan.domain.companion.entity.CompanionApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanionApplicationRepository extends JpaRepository<CompanionApplication, UUID> {

    // 특정 모집글의 신청 목록 (방장용 관리 화면)
    List<CompanionApplication> findByCompanion_CompanionIdOrderByCreatedAtDesc(UUID companionId);

    // 중복 신청 방지 — PENDING/APPROVED 같은 활성 상태가 이미 있는지 확인
    // (REJECTED/CANCELED 이력은 남겨두고 재신청은 허용한다)
    boolean existsByCompanion_CompanionIdAndApplicant_IdAndStatusIn(
            UUID companionId, UUID applicantId, List<String> activeStatuses);

    Optional<CompanionApplication> findByCompanion_CompanionIdAndApplicant_Id(UUID companionId, UUID applicantId);

    // 승인된 신청 수 (마감 시 최소 인원 충족 여부 판단용)
    long countByCompanion_CompanionIdAndStatus(UUID companionId, String status);

    // 내가 참여한(승인된) 방 목록
    List<CompanionApplication> findByApplicant_IdAndStatusOrderByCreatedAtDesc(UUID applicantId, String status);
}
