package com.capstone.travelbusan.domain.notification.repository;

import com.capstone.travelbusan.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FcmTokenRepository extends JpaRepository<FcmToken, UUID> {
    Optional<FcmToken> findByUser_Id(UUID userId);
}