package com.capstone.travelbusan.domain.notification.entity;

import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fcm_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FcmToken {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String token;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static FcmToken of(User user, String token) {
        FcmToken fcmToken = new FcmToken();
        fcmToken.user = user;
        fcmToken.token = token;
        fcmToken.updatedAt = LocalDateTime.now();
        return fcmToken;
    }

    public void updateToken(String token) {
        this.token = token;
        this.updatedAt = LocalDateTime.now();
    }
}