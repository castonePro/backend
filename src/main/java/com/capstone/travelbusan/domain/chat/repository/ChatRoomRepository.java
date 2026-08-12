package com.capstone.travelbusan.domain.chat.repository;

import com.capstone.travelbusan.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    // 사용자의 채팅방 목록
    @Query("SELECT r FROM ChatRoom r WHERE r.user.id = :userId OR r.guide.id = :userId ORDER BY r.createdAt DESC")
    List<ChatRoom> findByUserId(@Param("userId") UUID userId);

    // 이미 채팅방이 있는지 체크
    boolean existsByUserBid_BidIdAndUser_IdAndGuide_Id(UUID bidId, UUID userId, UUID guideId);
}