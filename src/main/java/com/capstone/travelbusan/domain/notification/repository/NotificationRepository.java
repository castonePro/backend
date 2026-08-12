package com.capstone.travelbusan.domain.notification.repository;

import com.capstone.travelbusan.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByReceiver_IdOrderByCreatedAtDesc(UUID receiverId);
    long countByReceiver_IdAndIsReadFalse(UUID receiverId);
}
