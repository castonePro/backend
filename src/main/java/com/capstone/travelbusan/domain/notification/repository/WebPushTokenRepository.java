package com.capstone.travelbusan.domain.notification.repository;

import com.capstone.travelbusan.domain.notification.entity.WebPushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebPushTokenRepository extends JpaRepository<WebPushToken, UUID> {

    Optional<WebPushToken> findByToken(String token);

    List<WebPushToken> findAllByUser_Id(UUID userId);

    void deleteByToken(String token);
}
