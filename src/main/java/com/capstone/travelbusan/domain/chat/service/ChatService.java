package com.capstone.travelbusan.domain.chat.service;

import com.capstone.travelbusan.domain.chat.dto.ChatDto;
import com.capstone.travelbusan.domain.chat.entity.ChatMessage;
import com.capstone.travelbusan.domain.chat.entity.ChatRoom;
import com.capstone.travelbusan.domain.chat.repository.ChatMessageRepository;
import com.capstone.travelbusan.domain.chat.repository.ChatRoomRepository;
import com.capstone.travelbusan.domain.notification.service.FcmService;
import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.user.repository.UserRepository;
import com.capstone.travelbusan.domain.userbid.entity.UserBid;
import com.capstone.travelbusan.domain.userbid.repository.UserBidRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final UserBidRepository userBidRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FcmService fcmService;

    // 채팅방 생성 (가이드 선택 시)
    @Transactional
    public ChatDto.RoomResponse createRoom(UUID bidId, UUID userId, UUID guideId) {
        // 이미 채팅방이 있으면 기존 반환
        if (chatRoomRepository.existsByUserBid_BidIdAndUser_IdAndGuide_Id(bidId, userId, guideId)) {
            ChatRoom existing = chatRoomRepository.findByUserId(userId).stream()
                    .filter(r -> r.getGuide().getId().equals(guideId))
                    .findFirst()
                    .orElseThrow();
            return ChatDto.RoomResponse.from(existing, 0, "");
        }

        User user = findUser(userId);
        User guide = findUser(guideId);
        UserBid bid = userBidRepository.findById(bidId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다."));

        ChatRoom room = ChatRoom.builder()
                .userBid(bid)
                .user(user)
                .guide(guide)
                .build();

        ChatRoom saved = chatRoomRepository.save(room);

        // 가이드에게 알림
        fcmService.sendNotification(guideId, "새 채팅방", user.getNickname() + "님이 채팅을 시작했습니다.");

        return ChatDto.RoomResponse.from(saved, 0, "");
    }

    // 내 채팅방 목록
    public List<ChatDto.RoomResponse> getMyRooms(UUID userId) {
        return chatRoomRepository.findByUserId(userId).stream()
                .map(room -> {
                    long unread = chatMessageRepository
                            .countByChatRoom_RoomIdAndIsReadFalseAndSender_IdNot(room.getRoomId(), userId);
                    List<ChatMessage> messages = chatMessageRepository
                            .findByChatRoom_RoomIdOrderByCreatedAtAsc(room.getRoomId());
                    String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContent();
                    return ChatDto.RoomResponse.from(room, unread, lastMessage);
                })
                .collect(Collectors.toList());
    }

    // 메시지 목록 조회 + 읽음 처리
    @Transactional
    public List<ChatDto.MessageResponse> getMessages(UUID roomId, UUID userId) {
        chatMessageRepository.markAllAsRead(roomId, userId);
        return chatMessageRepository.findByChatRoom_RoomIdOrderByCreatedAtAsc(roomId)
                .stream()
                .map(ChatDto.MessageResponse::from)
                .collect(Collectors.toList());
    }

    // 메시지 전송 (WebSocket)
    @Transactional
    public ChatDto.MessageResponse sendMessage(UUID roomId, UUID senderId, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (room.getIsClosed()) {
            throw new IllegalStateException("종료된 채팅방입니다.");
        }

        User sender = findUser(senderId);
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        ChatDto.MessageResponse response = ChatDto.MessageResponse.from(saved);

        // WebSocket으로 실시간 전송
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, response);

        // 상대방 FCM 알림
        UUID receiverId = room.getUser().getId().equals(senderId)
                ? room.getGuide().getId()
                : room.getUser().getId();
        fcmService.sendNotification(receiverId, sender.getNickname(), content);

        return response;
    }

    // 채팅방 나가기
    @Transactional
    public void leaveRoom(UUID roomId, UUID userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        room.close();
        chatRoomRepository.save(room);

        // 상대방 알림
        UUID otherId = room.getUser().getId().equals(userId)
                ? room.getGuide().getId()
                : room.getUser().getId();
        User user = findUser(userId);
        fcmService.sendNotification(otherId, "채팅방 종료", user.getNickname() + "님이 채팅방을 나갔습니다.");

        // WebSocket으로 나가기 알림
        messagingTemplate.convertAndSend("/topic/chat/" + roomId + "/leave", userId);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public ChatDto.RoomResponse createDirectRoom(UUID userId, UUID guideId) {
        User user = findUser(userId);
        User guide = findUser(guideId);

        // 이미 채팅방 있으면 기존 반환
        List<ChatRoom> existing = chatRoomRepository.findByUserId(userId);
        for (ChatRoom room : existing) {
            if (room.getGuide().getId().equals(guideId) && !room.getIsClosed()) {
                long unread = chatMessageRepository
                        .countByChatRoom_RoomIdAndIsReadFalseAndSender_IdNot(room.getRoomId(), userId);
                return ChatDto.RoomResponse.from(room, unread, "");
            }
        }

        ChatRoom room = ChatRoom.builder()
                .user(user)
                .guide(guide)
                .build();

        ChatRoom saved = chatRoomRepository.save(room);
        fcmService.sendNotification(guideId, "새 문의", user.getNickname() + "님이 문의를 시작했습니다.");
        return ChatDto.RoomResponse.from(saved, 0, "");
    }
}