package com.capstone.travelbusan.domain.notification.service;

import com.capstone.travelbusan.domain.notification.dto.NotificationDto;
import com.capstone.travelbusan.domain.notification.entity.FcmToken;
import com.capstone.travelbusan.domain.notification.entity.WebPushToken;
import com.capstone.travelbusan.domain.notification.repository.FcmTokenRepository;
import com.capstone.travelbusan.domain.notification.repository.NotificationRepository;
import com.capstone.travelbusan.domain.notification.repository.WebPushTokenRepository;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.google.firebase.messaging.WebpushNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final WebPushTokenRepository webPushTokenRepository;

    /** 웹 푸시 알림 클릭 시 열 주소와 아이콘의 기준 URL (HTTPS 절대 주소여야 함) */
    @Value("${app.web.base-url:https://travelbusan.site}")
    private String webBaseUrl;

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

        // 웹(브라우저) 푸시 — 앱 토큰과 별도 테이블. 등록된 브라우저 전부에 보낸다.
        sendWebPush(receiverId, title, body);
    }

    // ───────────────────────── 웹 푸시 ─────────────────────────

    /** 웹 푸시 토큰 등록. 같은 브라우저(토큰)가 다른 계정으로 로그인하면 주인을 바꾼다. */
    @Transactional
    public void saveWebToken(UUID userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        webPushTokenRepository.findByToken(token)
                .ifPresentOrElse(
                        existing -> existing.reassign(user),
                        () -> webPushTokenRepository.save(WebPushToken.of(user, token))
                );
    }

    /** 로그아웃 시 해당 브라우저의 웹 푸시 토큰 삭제 (본인 토큰만) */
    @Transactional
    public void deleteWebToken(UUID userId, String token) {
        webPushTokenRepository.findByToken(token)
                .filter(t -> t.getUser().getId().equals(userId))
                .ifPresent(webPushTokenRepository::delete);
    }

    private void sendWebPush(UUID receiverId, String title, String body) {
        List<WebPushToken> tokens = webPushTokenRepository.findAllByUser_Id(receiverId);
        if (tokens.isEmpty()) return;

        String base = webBaseUrl.endsWith("/") ? webBaseUrl.substring(0, webBaseUrl.length() - 1) : webBaseUrl;
        WebpushConfig webpush = WebpushConfig.builder()
                .setNotification(WebpushNotification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .setIcon(base + "/images/brand/logo.png")
                        .build())
                .setFcmOptions(WebpushFcmOptions.withLink(base + "/notifications"))
                .build();

        for (WebPushToken webToken : tokens) {
            try {
                Message message = Message.builder()
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .setWebpushConfig(webpush)
                        .setToken(webToken.getToken())
                        .build();
                FirebaseMessaging.getInstance().send(message);
            } catch (FirebaseMessagingException e) {
                // 브라우저에서 알림을 끄거나 사이트 데이터를 지우면 토큰이 무효가 된다 → 정리
                MessagingErrorCode code = e.getMessagingErrorCode();
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    webPushTokenRepository.delete(webToken);
                    log.info("만료된 웹 푸시 토큰 삭제: {}", receiverId);
                } else {
                    log.error("웹 푸시 전송 실패: {}", e.getMessage());
                }
            } catch (Exception e) {
                // Firebase 미초기화 등 — 알림 이력 저장/다른 기능에는 영향 없이 로그만 남긴다
                log.error("웹 푸시 전송 실패: {}", e.getMessage());
            }
        }
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