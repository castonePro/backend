package com.capstone.travelbusan.domain.notification.entity;

import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 웹(브라우저) 푸시용 FCM 토큰.
 *
 * <p>기존 {@code fcm_tokens}(모바일 앱용)는 사용자당 1개만 저장해서, 웹 토큰을 같은 테이블에 넣으면
 * 앱 토큰을 덮어써 앱 푸시가 끊긴다. 그래서 웹 토큰은 이 테이블에 따로 두고,
 * 한 사용자가 여러 브라우저(집 PC, 학교 노트북 등)를 쓸 수 있도록 토큰 단위로 저장한다.
 * 앱 쪽 테이블/코드는 건드리지 않는다.
 */
@Entity
@Table(name = "web_push_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebPushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "token_id", length = 36)
    private UUID tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static WebPushToken of(User user, String token) {
        WebPushToken t = new WebPushToken();
        t.user = user;
        t.token = token;
        t.createdAt = LocalDateTime.now();
        t.updatedAt = t.createdAt;
        return t;
    }

    /** 같은 브라우저에서 다른 계정으로 로그인한 경우 토큰 주인을 바꾼다 */
    public void reassign(User user) {
        this.user = user;
        this.updatedAt = LocalDateTime.now();
    }
}
