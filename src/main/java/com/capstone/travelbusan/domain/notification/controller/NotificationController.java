package com.capstone.travelbusan.domain.notification.controller;

import com.capstone.travelbusan.domain.notification.dto.NotificationDto;
import com.capstone.travelbusan.domain.notification.service.FcmService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final FcmService fcmService;

    // 내 알림 목록 (벨 아이콘 클릭 시)
    @GetMapping
    public ResponseEntity<List<NotificationDto.Response>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(fcmService.getMyNotifications(currentUser.getUserId()));
    }

    // 읽지 않은 알림 수 (벨 아이콘 배지)
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(Map.of("unreadCount", fcmService.getUnreadCount(currentUser.getUserId())));
    }

    // 알림 하나 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID notificationId) {
        fcmService.markAsRead(currentUser.getUserId(), notificationId);
        return ResponseEntity.noContent().build();
    }

    // 전체 읽음 처리
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserPrincipal currentUser) {
        fcmService.markAllAsRead(currentUser.getUserId());
        return ResponseEntity.noContent().build();
    }
}
