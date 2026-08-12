package com.capstone.travelbusan.domain.notification.service;

import com.capstone.travelbusan.domain.notification.dto.NotificationDto;
import com.capstone.travelbusan.domain.notification.entity.FcmToken;
import com.capstone.travelbusan.domain.notification.repository.FcmTokenRepository;
import com.capstone.travelbusan.domain.notification.repository.NotificationRepository;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    // FCM 토큰 저장/갱신
    @Transactional
    public void saveToken(UUID userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        fcmTokenRepository.findByUser_Id(userId)
                .ifPresentOrElse(
                        fcmToken -> fcmToken.updateToken(token),
                        () -> fcmTokenRepository.save(FcmToken.of(user, token))
                );
    }

    // 알림 전송 — 인앱 알림 이력 저장(항상) + 푸시 발송(토큰 있을 때만)
    // 기존 호출부(BidApplicationService, ChatService, CompanionService 등)는 그대로 재사용
    @Transactional
    public void sendNotification(UUID receiverId, String title, String body) {
        userRepository.findById(receiverId).ifPresent(receiver -> {
            com.capstone.travelbusan.domain.notification.entity.Notification notification =
                    com.capstone.travelbusan.domain.notification.entity.Notification.builder()
                            .receiver(receiver)
                            .title(title)
                            .body(body)
                            .build();
            notificationRepository.save(notification);
        });

        fcmTokenRepository.findByUser_Id(receiverId).ifPresent(fcmToken -> {
            try {
                Message message = Message.builder()
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .setToken(fcmToken.getToken())
                        .build();

                FirebaseMessaging.getInstance().send(message);
                log.info("FCM 알림 전송 성공: {}", receiverId);
            } catch (Exception e) {
                log.error("FCM 알림 전송 실패: {}", e.getMessage());
            }
        });
    }

    // 내 알림 목록
    public List<NotificationDto.Response> getMyNotifications(UUID userId) {
        return notificationRepository.findByReceiver_IdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationDto.Response::from)
                .toList();
    }

    // 읽지 않은 알림 수 (벨 아이콘 배지용)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByReceiver_IdAndIsReadFalse(userId);
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        com.capstone.travelbusan.domain.notification.entity.Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 알림만 읽음 처리할 수 있습니다.");
        }
        notification.markAsRead();
    }

    // 전체 읽음 처리
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.findByReceiver_IdOrderByCreatedAtDesc(userId)
                .forEach(com.capstone.travelbusan.domain.notification.entity.Notification::markAsRead);
    }
}