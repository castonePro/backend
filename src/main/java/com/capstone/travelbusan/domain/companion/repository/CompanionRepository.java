package com.capstone.travelbusan.domain.companion.repository;

import com.capstone.travelbusan.domain.companion.entity.Companion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompanionRepository extends JpaRepository<Companion, UUID> {

    // 동행 탐색: 특정 상태(기본 모집중)의 모집글 목록
    List<Companion> findByStatusOrderByCreatedAtDesc(String status);

    // 내가 만든 방 목록
    List<Companion> findByHost_IdOrderByCreatedAtDesc(UUID hostId);
}
