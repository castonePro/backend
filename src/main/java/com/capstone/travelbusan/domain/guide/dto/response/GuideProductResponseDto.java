package com.capstone.travelbusan.domain.guide.dto.response;

import com.capstone.travelbusan.domain.guide.entity.GuideProduct;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class GuideProductResponseDto {
    private String serviceId;
    private String guideName;
    private String title;
    private String description;
    private String region;
    private Integer durationMinutes;
    private Integer maxCapacity;
    private BigDecimal pricePerPerson;
    private Boolean hasCar;
    private List<String> availableLanguages;
    private String meetingPoint;
    private String meetingPointDesc;
    private List<String> includedItems;
    private List<String> excludedItems;
    private List<String> relatedMaterials;
    private Boolean isPublished;
    private String guideUserId;

    public static GuideProductResponseDto from(GuideProduct product) {
        return GuideProductResponseDto.builder()
                .serviceId(product.getServiceId().toString())
                .guideName(product.getGuider().getUser().getNickname())
                .title(product.getTitle())
                .description(product.getDescription())
                .region(product.getRegion())
                .durationMinutes(product.getDurationMinutes())
                .maxCapacity(product.getMaxCapacity())
                .pricePerPerson(product.getPricePerPerson())
                .hasCar(product.getHasCar())
                .availableLanguages(product.getAvailableLanguages())
                .meetingPoint(product.getMeetingPoint())
                .meetingPointDesc(product.getMeetingPointDesc())
                .includedItems(product.getIncludedItems())
                .excludedItems(product.getExcludedItems())
                .relatedMaterials(product.getRelatedMaterials())
                .isPublished(product.getIsPublished())
                .guideUserId(product.getGuider().getGuideId().toString())
                .build();
    }
}
