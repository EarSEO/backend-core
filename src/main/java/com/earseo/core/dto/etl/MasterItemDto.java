package com.earseo.core.dto.etl;

import com.earseo.core.entity.EnMaster;
import com.earseo.core.entity.KoMaster;
import lombok.Builder;
import org.locationtech.jts.geom.Point;

@Builder
public record MasterItemDto(String contentId, String contentTypeId, String cat1,
                            String cat2, String cat1Code, String cat2Code, String title,
                            String addr1, String addr2, String addr3,
                            Double mapX, Double mapY, String modifiedtime, String tel, Integer mLevel, String overview,
                            String originImgUrl, String smallImgUrl, String usetime, String restdate, String parking, String usefee) {

    public KoMaster toKo() {
        return KoMaster.builder()
                .contentId(contentId)
                .contentTypeId(contentTypeId)
                .cat1(cat1)
                .cat2(cat2)
                .cat1Code(cat1Code)
                .cat2Code(cat2Code)
                .title(title)
                .addr1(addr1)
                .addr2(addr2)
                .addr3(addr3)
                .mapX(mapX)
                .mapY(mapY)
                .modifiedtime(modifiedtime)
                .tel(tel)
                .mLevel(mLevel)
                .overview(overview)
                .originImgUrl(originImgUrl)
                .smallImgUrl(smallImgUrl)
                .usetime(usetime)
                .restdate(restdate)
                .parking(parking)
                .usefee(usefee)
                .build();
    }

    public EnMaster toEn() {
        return EnMaster.builder()
                .contentId(contentId)
                .contentTypeId(contentTypeId)
                .cat1(cat1)
                .cat2(cat2)
                .cat1Code(cat1Code)
                .cat2Code(cat2Code)
                .title(title)
                .addr1(addr1)
                .addr2(addr2)
                .addr3(addr3)
                .mapX(mapX)
                .mapY(mapY)
                .modifiedtime(modifiedtime)
                .tel(tel)
                .mLevel(mLevel)
                .overview(overview)
                .originImgUrl(originImgUrl)
                .smallImgUrl(smallImgUrl)
                .usetime(usetime)
                .restdate(restdate)
                .parking(parking)
                .usefee(usefee)
                .build();
    }
}
