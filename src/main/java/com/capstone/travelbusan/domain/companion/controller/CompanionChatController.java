package com.capstone.travelbusan.domain.companion.controller;

import com.capstone.travelbusan.domain.companion.dto.CompanionChatDto;
import com.capstone.travelbusan.domain.companion.service.CompanionChatService;
import com.capstone.travelbusan.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CompanionChatController {

    private final CompanionChatService companionChatService;

    // ==================== REST ====================

    // 동행 그룹 채팅 메시지 목록
    @GetMapping("/api/v1/companions/{companionId}/chat/messages")
    public ResponseEntity<List<CompanionChatDto.MessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID companionId) {
        return ResponseEntity.ok(companionChatService.getMessages(companionId, currentUser.getUserId()));
    }

    // ==================== WebSocket ====================

    // 메시지 전송 (/app/companion-chat/{companionId}) — 기존 WebSocketConfig(/ws/chat) 재사용
    @MessageMapping("/companion-chat/{companionId}")
    public void sendMessage(
            @DestinationVariable UUID companionId,
            @Payload CompanionChatDto.MessageRequest request) {
        companionChatService.sendMessage(companionId, UUID.fromString(request.getSenderId()), request.getContent());
    }
}
