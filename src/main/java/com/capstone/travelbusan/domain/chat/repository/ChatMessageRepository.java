package com.capstone.travelbusan.domain.chat.repository;

import com.capstone.travelbusan.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    // 채팅방 메시지 전체 조회 (시간순)
    List<ChatMessage> findByChatRoom_RoomIdOrderByCreatedAtAsc(UUID roomId);

    // 읽지 않은 메시지 수
    long countByChatRoom_RoomIdAndIsReadFalseAndSender_IdNot(UUID roomId, UUID userId);

    // 읽음 처리
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.chatRoom.roomId = :roomId AND m.sender.id != :userId AND m.isRead = false")
    void markAllAsRead(@Param("roomId") UUID roomId, @Param("userId") UUID userId);
}