package com.capstone.travelbusan.domain.notification.entity;

import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 인앱 알림 기록.
 * 기존 FcmService.sendNotification()은 푸시(FCM)만 보내고 이력을 남기지 않아
 * 벨 아이콘을 눌러도 보여줄 목록이 없었다. 이 엔티티가 그 이력을 담당한다.
 */
@Entity
@Table(name = "notifications")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private UUID notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void markAsRead() {
        this.isRead = true;
    }
}
