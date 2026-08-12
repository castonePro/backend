package com.capstone.travelbusan.domain.companion.dto;

import com.capstone.travelbusan.domain.companion.entity.CompanionChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public class CompanionChatDto {

    // 메시지 전송 요청 — 기존 ChatDto.MessageRequest와 동일한 payload 형태를 유지해
    // Flutter 쪽 STOMP 전송 코드를 그대로 재사용할 수 있게 한다.
    @Getter
    public static class MessageRequest {
        private String content;
        private String senderId;
    }

    // 메시지 응답
    @Getter
    @Builder
    public static class MessageResponse {
        private UUID messageId;
        private UUID companionId;
        private UUID senderId;
        private String senderNickname;
        private String content;
        private LocalDateTime createdAt;

        public static MessageResponse from(CompanionChatMessage message) {
            return MessageResponse.builder()
                    .messageId(message.getMessageId())
                    .companionId(message.getCompanion().getCompanionId())
                    .senderId(message.getSender().getId())
                    .senderNickname(message.getSender().getNickname())
                    .content(message.getContent())
                    .createdAt(message.getCreatedAt())
                    .build();
        }
    }
}
