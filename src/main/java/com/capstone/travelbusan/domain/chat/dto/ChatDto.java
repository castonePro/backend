package com.capstone.travelbusan.domain.chat.dto;

import com.capstone.travelbusan.domain.chat.entity.ChatMessage;
import com.capstone.travelbusan.domain.chat.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatDto {

    // 메시지 전송 요청
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
        private UUID roomId;
        private UUID senderId;
        private String senderNickname;
        private String content;
        private Boolean isRead;
        private LocalDateTime createdAt;

        public static MessageResponse from(ChatMessage message) {
            return MessageResponse.builder()
                    .messageId(message.getMessageId())
                    .roomId(message.getChatRoom().getRoomId())
                    .senderId(message.getSender().getId())
                    .senderNickname(message.getSender().getNickname())
                    .content(message.getContent())
                    .isRead(message.getIsRead())
                    .createdAt(message.getCreatedAt())
                    .build();
        }
    }

    // 채팅방 응답
    @Getter
    @Builder
    public static class RoomResponse {
        private UUID roomId;
        private UUID userId;
        private String userNickname;
        private UUID guideId;
        private String guideNickname;
        private Boolean isClosed;
        private LocalDateTime createdAt;
        private long unreadCount; // 읽지 않은 메시지 수
        private String lastMessage; // 마지막 메시지 미리보기

        public static RoomResponse from(ChatRoom room, long unreadCount, String lastMessage) {
            return RoomResponse.builder()
                    .roomId(room.getRoomId())
                    .userId(room.getUser().getId())
                    .userNickname(room.getUser().getNickname())
                    .guideId(room.getGuide().getId())
                    .guideNickname(room.getGuide().getNickname())
                    .isClosed(room.getIsClosed())
                    .createdAt(room.getCreatedAt())
                    .unreadCount(unreadCount)
                    .lastMessage(lastMessage)
                    .build();
        }
    }
}