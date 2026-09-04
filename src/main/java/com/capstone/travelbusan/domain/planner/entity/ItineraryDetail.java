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

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    // Oracle 호환: 콤마 구분자("자연,공원")로 VARCHAR2 컬럼에 저장
    @Convert(converter = com.capstone.travelbusan.global.converter.StringListConverter.class)
    @Column(name = "category_type", length = 500)
    private List<String> categoryType;

    @Column(name = "operating_hours")
    private String operatingHours;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "place_id")
    private Long placeId; // 원본 장소 참조 (Nullable)

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    private Double latitude;
    private Double longitude;
}