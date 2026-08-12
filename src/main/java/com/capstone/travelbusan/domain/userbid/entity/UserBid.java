package com.capstone.travelbusan.domain.userbid.entity;

import com.capstone.travelbusan.domain.planner.entity.Itinerary;
import com.capstone.travelbusan.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_bids")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserBid {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "bid_id")
    private UUID bidId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING / ACCEPTED / REJECTED

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private Boolean isClosed = false;

    public void close() {
        this.isClosed = true;
    }
}