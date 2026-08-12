package com.capstone.travelbusan.domain.companion.entity;

import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 동행 그룹 채팅 메시지.
 * 기존 chat 도메인의 ChatRoom은 user/guide 2인 구조라 다자간 동행 그룹에는 맞지 않는다.
 * 별도 "방" 엔티티 없이 Companion 자체를 그룹방으로 취급하고, 멤버 자격은
 * 방장이거나 APPROVED 상태의 CompanionApplication이 있는지로 그때그때 판별한다.
 */
@Entity
@Table(name = "companion_chat_messages")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CompanionChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "message_id")
    private UUID messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companion_id", nullable = false)
    private Companion companion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
