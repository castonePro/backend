package com.capstone.travelbusan.domain.recommend_place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlaceDetailDto {
    private Integer placeId;
    private String title;
    private String addr1;
    private String firstImage;
    private String firstImage2;
    private String cat1;
    private String cat2;
    private String cat3;
    private String homepage;
    private String overview; // null이면 "상세 설명이 없습니다." 처리
}