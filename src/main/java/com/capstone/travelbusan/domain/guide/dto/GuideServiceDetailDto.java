package com.capstone.travelbusan.domain.guide.dto;

import com.capstone.travelbusan.domain.guide.entity.GuideServiceDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuideServiceDetailDto {
    private Integer sequenceOrder;
    private String startTime;
    private String endTime;
    private String location;
    private String content;

    public static GuideServiceDetailDto from(GuideServiceDetail detail) {
        return GuideServiceDetailDto.builder()
                .sequenceOrder(detail.getSequenceOrder())
                .startTime(detail.getStartTime().toString())
                .endTime(detail.getEndTime().toString())
                .location(detail.getLocation())
                .content(detail.getContent())
                .build();
    }
}
