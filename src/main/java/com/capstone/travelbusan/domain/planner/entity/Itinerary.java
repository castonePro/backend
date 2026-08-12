package com.capstone.travelbusan.domain.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "itineraries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "itinerary_id")
    private Long itineraryId;

    @Column(name = "user_id", nullable = false)
    private UUID userId; // 유저 식별자 (UUID)

    @Column(nullable = false)
    private String title;

    private String region;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 상세 코스와의 1:N 관계 (양방향)
    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItineraryDetail> details = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}