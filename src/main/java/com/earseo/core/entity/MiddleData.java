package com.earseo.core.entity;

import com.earseo.core.dto.etl.MiddleDataDto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "middle_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiddleData {

    @Id
    @Column(name = "content_id", nullable = false, unique = true)
    private String contentId;

    @Column(name = "content_type_id")
    private String contentTypeId;

    @Column(name = "cat1")
    private String cat1;

    @Column(name = "cat2")
    private String cat2;

    @Column(name = "cat3")
    private String cat3;

    @Column(name = "outl", columnDefinition = "TEXT")
    private String outl;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "addr1", columnDefinition = "TEXT")
    private String addr1;

    @Column(name = "addr2", columnDefinition = "TEXT")
    private String addr2;

    @Column(name = "map_x", columnDefinition = "TEXT")
    private String mapX;

    @Column(name = "map_y", columnDefinition = "TEXT")
    private String mapY;

    @Column(name = "modified_time", columnDefinition = "TEXT")
    private String modifiedtime;

    @Column(name = "tel", columnDefinition = "TEXT")
    private String tel;

    @Column(name = "m_level", columnDefinition = "TEXT")
    private String mLevel;

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


    public MiddleData(MiddleDataDto dto) {
        this.contentId = dto.contentId();
        this.contentTypeId = dto.contentTypeId();
        this.cat1 = dto.cat1();
        this.cat2 = dto.cat2();
        this.cat3 = dto.cat3();
        this.outl = dto.outl();
        this.title = dto.title();
        this.addr1 = dto.addr1();
        this.addr2 = dto.addr2();
        this.mapX = dto.mapX();
        this.mapY = dto.mapY();
        this.modifiedtime = dto.modifiedtime();
        this.tel = dto.tel();
        this.mLevel = dto.mLevel();
        this.overview = dto.overview();
        this.originImgUrl = dto.originImgUrl();
        this.smallImgUrl = dto.smallImgUrl();
        this.usetime = dto.usetime();
        this.restdate = dto.restdate();
        this.parking = dto.parking();
        this.usefee = dto.usefee();
    }
}
