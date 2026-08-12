package com.capstone.travelbusan.domain.recommend_place.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "travel_places")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlace {

    @Id
    @Column(name = "place_id")
    private Integer placeId;

    @Column(name = "title")
    private String title;

    @Column(name = "addr1")
    private String addr1;

    @Column(name = "first_image")
    private String firstImage;

    @Column(name = "first_image2")
    private String firstImage2;

    @Column(name = "cat1")
    private String cat1;

    @Column(name = "cat2")
    private String cat2;

    @Column(name = "cat3")
    private String cat3;
}