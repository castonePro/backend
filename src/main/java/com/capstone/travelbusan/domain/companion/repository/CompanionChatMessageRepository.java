package com.capstone.travelbusan.domain.companion.repository;

import com.capstone.travelbusan.domain.companion.entity.CompanionChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompanionChatMessageRepository extends JpaRepository<CompanionChatMessage, UUID> {
    List<CompanionChatMessage> findByCompanion_CompanionIdOrderByCreatedAtAsc(UUID companionId);
}
