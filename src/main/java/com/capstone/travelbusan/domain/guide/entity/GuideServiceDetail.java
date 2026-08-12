package com.capstone.travelbusan.domain.guide.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "guide_service_info_detail")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GuideServiceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "detail_id")
    private UUID detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private GuideProduct service;

    @Column(nullable = false)
    private Integer sequenceOrder;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
