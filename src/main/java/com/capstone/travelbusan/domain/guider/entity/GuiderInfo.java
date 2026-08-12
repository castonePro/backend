package com.capstone.travelbusan.domain.guider.entity;
import com.capstone.travelbusan.domain.guider.dto.request.LanguageScoreDto;
import com.capstone.travelbusan.domain.user.entity.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "guider_info")
@Getter @Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GuiderInfo {

    @Id
    @Column(name = "guide_id")
    private UUID guideId;

    // ★ 핵심: users 테이블의 PK를 자신의 PK이자 FK로 사용
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "guide_id")
    private User user;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "varchar(50)[]", nullable = false)
    private List<String> activeRegions;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "varchar(50)[]", nullable = false)
    private List<String> availableLanguages;

    @Column(nullable = false, length = 20)
    private String experiencePeriod;

    // 어학 성적 리스트 (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<LanguageScoreDto> languageScores;

    @Column(length = 500)
    private String introduction;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "varchar(50)[]", nullable = false)
    private List<String> specialties;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal certificationScore = BigDecimal.ZERO;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal reviewScore = BigDecimal.ZERO;
}