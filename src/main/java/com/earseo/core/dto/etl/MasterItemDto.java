package com.earseo.core.dto.etl;

import org.locationtech.jts.geom.Point;

public record MasterItemDto(String contentId, String contentTypeId, String cat1,
                            String cat2, String cat3, String ocat1, String ocat2, String ocat3,
                            String outl, String title, String addr1, String addr2, String addr3,
                            Double mapX, Double mapY, String modifiedtime, String tel, Integer mLevel, String overview,
                            String originImgUrl, String smallImgUrl, String usetime, String restdate, String parking, String usefee) {
}
