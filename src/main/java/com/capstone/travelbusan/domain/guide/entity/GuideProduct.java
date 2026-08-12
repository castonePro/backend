package com.capstone.travelbusan.domain.guide.entity;

import com.capstone.travelbusan.domain.guider.entity.GuiderInfo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "guide_service_info")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GuideProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_id")
    private UUID serviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private GuiderInfo guider;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerPerson;

    @Column
    @Builder.Default
    private Boolean hasCar = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "varchar(50)[]", nullable = false)
    private List<String> availableLanguages;

    @Column(nullable = false, length = 255)
    private String meetingPoint;

    @Column(columnDefinition = "TEXT")
    private String meetingPointDesc;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> includedItems;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> excludedItems;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> relatedMaterials;

    @Column
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_published")
    @Builder.Default
    private Boolean isPublished = false;
}
