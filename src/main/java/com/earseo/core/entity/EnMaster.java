package com.earseo.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter @Builder
public class EnMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id")
    private String contentId;

    @Column(name = "content_type_id")
    private String contentTypeId;

    @Column(name = "cat1")
    private String cat1;

    @Column(name = "cat2")
    private String cat2;

    @Column(name = "cat1_code")
    private String cat1Code;

    @Column(name = "cat2_code")
    private String cat2Code;

    @Column(name = "title")
    private String title;

    @Column(name = "addr1")
    private String addr1;

    @Column(name = "addr2")
    private String addr2;

    @Column(name = "addr3")
    private String addr3;

    @Column(name = "map_x")
    private Double mapX;

    @Column(name = "map_y")
    private Double mapY;

    @Column(name = "modifiedtime")
    private String modifiedtime;

    @Column(name = "tel", columnDefinition = "TEXT")
    private String tel;

    @Column(name = "m_level")
    private Integer mLevel;

    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    @Column(name = "origin_img_url", columnDefinition = "TEXT")
    private String originImgUrl;

    @Column(name = "small_img_url", columnDefinition = "TEXT")
    private String smallImgUrl;

    @Column(name = "use_time", columnDefinition = "TEXT")
    private String usetime;

    @Column(name = "rest_date", columnDefinition = "TEXT")
    private String restdate;

    @Column(name = "parking", columnDefinition = "TEXT")
    private String parking;

    @Column(name = "use_fee", columnDefinition = "TEXT")
    private String usefee;

}