package com.capstone.travelbusan.domain.bidapplication.entity;

import com.capstone.travelbusan.domain.userbid.entity.UserBid;
import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bid_applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bid_id", "guide_id"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BidApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "application_id")
    private UUID applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private UserBid userBid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private User guide;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}