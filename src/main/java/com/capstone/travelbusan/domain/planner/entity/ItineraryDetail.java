package com.capstone.travelbusan.domain.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "itinerary_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary; // 외래 키 매핑

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "start_time", length = 20)
    private LocalTime startTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "place_name", nullable = false, length = 255)
    private String placeName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_type")
    private List<String> categoryType;

    @Column(name = "operating_hours", length = 255)
    private String operatingHours;

    @Lob
    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(name = "description")
    private String description;

    @Column(name = "place_id")
    private Long placeId; // 원본 장소 참조 (Nullable)

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}