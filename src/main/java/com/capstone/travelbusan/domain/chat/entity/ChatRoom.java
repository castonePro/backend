package com.capstone.travelbusan.domain.chat.entity;

import com.capstone.travelbusan.domain.user.entity.User;
import com.capstone.travelbusan.domain.userbid.entity.UserBid;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_rooms")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "room_id")
    private UUID roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id")
    private UserBid userBid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private User guide;

    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private Boolean isClosed = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void close() {
        this.isClosed = true;
    }
}