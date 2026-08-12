package com.capstone.travelbusan.domain.companion.service;

import com.capstone.travelbusan.domain.companion.dto.CompanionChatDto;
import com.capstone.travelbusan.domain.companion.entity.Companion;
import com.capstone.travelbusan.domain.companion.entity.CompanionApplication;
import com.capstone.travelbusan.domain.companion.entity.CompanionChatMessage;
import com.capstone.travelbusan.domain.companion.repository.CompanionApplicationRepository;
import com.capstone.travelbusan.domain.companion.repository.CompanionChatMessageRepository;
import com.capstone.travelbusan.domain.companion.repository.CompanionRepository;
import com.capstone.travelbusan.domain.notification.service.FcmService;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanionChatService {

    private final CompanionRepository companionRepository;
    private final CompanionApplicationRepository applicationRepository;
    private final CompanionChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FcmService fcmService;

    // 메시지 목록 조회 (멤버만)
    public List<CompanionChatDto.MessageResponse> getMessages(UUID companionId, UUID userId) {
        Companion companion = findCompanion(companionId);
        requireMember(companion, userId);

        return chatMessageRepository.findByCompanion_CompanionIdOrderByCreatedAtAsc(companionId).stream()
                .map(CompanionChatDto.MessageResponse::from)
                .toList();
    }

    // 메시지 전송 (WebSocket)
    @Transactional
    public CompanionChatDto.MessageResponse sendMessage(UUID companionId, UUID senderId, String content) {
        Companion companion = findCompanion(companionId);
        requireMember(companion, senderId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        CompanionChatMessage message = CompanionChatMessage.builder()
                .companion(companion)
                .sender(sender)
                .content(content)
                .build();

        CompanionChatMessage saved = chatMessageRepository.save(message);
        CompanionChatDto.MessageResponse response = CompanionChatDto.MessageResponse.from(saved);

        // 실시간 브로드캐스트
        messagingTemplate.convertAndSend("/topic/companion-chat/" + companionId, response);

        // 발신자를 제외한 멤버 전원에게 FCM
        memberIds(companion).stream()
                .filter(id -> !id.equals(senderId))
                .forEach(id -> fcmService.sendNotification(id, sender.getNickname(), content));

        return response;
    }

    private Companion findCompanion(UUID companionId) {
        return companionRepository.findById(companionId)
                .orElseThrow(() -> new IllegalArgumentException("모집글을 찾을 수 없습니다."));
    }

    // 그룹 채팅 멤버 자격: 방장이거나 승인된 신청자
    private void requireMember(Companion companion, UUID userId) {
        boolean isHost = companion.getHost().getId().equals(userId);
        boolean isApprovedApplicant = applicationRepository.existsByCompanion_CompanionIdAndApplicant_IdAndStatusIn(
                companion.getCompanionId(), userId, List.of(CompanionApplication.STATUS_APPROVED));

        if (!isHost && !isApprovedApplicant) {
            throw new IllegalArgumentException("동행 그룹 채팅은 승인된 멤버만 이용할 수 있습니다.");
        }
    }

    private List<UUID> memberIds(Companion companion) {
        List<UUID> ids = applicationRepository
                .findByCompanion_CompanionIdOrderByCreatedAtDesc(companion.getCompanionId()).stream()
                .filter(app -> CompanionApplication.STATUS_APPROVED.equals(app.getStatus()))
                .map(app -> app.getApplicant().getId())
                .collect(java.util.stream.Collectors.toList());
        ids.add(companion.getHost().getId());
        return ids;
    }
}
