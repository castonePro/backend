package com.capstone.travelbusan.domain.notification.dto;

import com.capstone.travelbusan.domain.notification.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationDto {

    @Getter
    @Builder
    public static class Response {
        private UUID notificationId;
        private String title;
        private String body;
        private Boolean isRead; // primitive boolean 쓰면 Jackson이 "read"로 잘못 직렬화함 (필드명이 is로 시작하는 경우의 함정)
        private LocalDateTime createdAt;

        public static Response from(Notification notification) {
            return Response.builder()
                    .notificationId(notification.getNotificationId())
                    .title(notification.getTitle())
                    .body(notification.getBody())
                    .isRead(notification.getIsRead())
                    .createdAt(notification.getCreatedAt())
                    .build();
        }
    }
}
