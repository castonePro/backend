package com.capstone.travelbusan.domain.chat.controller;

import com.capstone.travelbusan.domain.chat.dto.ChatDto;
import com.capstone.travelbusan.domain.chat.service.ChatService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ==================== REST ====================

    // 내 채팅방 목록
    @GetMapping("/api/v1/chat/rooms")
    public ResponseEntity<List<ChatDto.RoomResponse>> getMyRooms(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(chatService.getMyRooms(currentUser.getUserId()));
    }

    // 메시지 목록 조회
    @GetMapping("/api/v1/chat/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatDto.MessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID roomId) {
        return ResponseEntity.ok(chatService.getMessages(roomId, currentUser.getUserId()));
    }

    // 채팅방 나가기
    @DeleteMapping("/api/v1/chat/rooms/{roomId}")
    public ResponseEntity<Void> leaveRoom(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID roomId) {
        chatService.leaveRoom(roomId, currentUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    // ==================== WebSocket ====================

    // 메시지 전송 (/app/chat/{roomId})
    @MessageMapping("/chat/{roomId}")
    public void sendMessage(
            @DestinationVariable UUID roomId,
            @Payload ChatDto.MessageRequest request) {
        chatService.sendMessage(roomId, UUID.fromString(request.getSenderId()), request.getContent());
    }

    // 가이드 탐색에서 바로 문의하기 (bid 없이 채팅방 생성)
    @PostMapping("/api/v1/chat/rooms/direct")
    public ResponseEntity<ChatDto.RoomResponse> createDirectRoom(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody Map<String, String> request) {
        UUID guideId = UUID.fromString(request.get("guideId"));
        return ResponseEntity.ok(
                chatService.createDirectRoom(currentUser.getUserId(), guideId));
    }
}