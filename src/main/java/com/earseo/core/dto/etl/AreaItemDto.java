package com.earseo.core.dto.etl;

import com.earseo.core.dto.tourApi.AreaResponse;

public record AreaItemDto(String contentId, String contentTypeId, String title, String address,
                          String detailAddress, String catCode, Double mapX, Double mapY,
                          String imageUrl, String thumbnailUrl, String tel, Integer mlevel, String modifiedtime) {

    public static AreaItemDto from(AreaResponse.Item item){
        return new AreaItemDto(
                item.contentid(), item.contenttypeid(), item.title(), item.addr1(), item.addr2(), item.cat2(),
                Double.parseDouble(item.mapx()), Double.parseDouble(item.mapy()), item.firstimage(), item.firstimage2(),
                item.tel(),item.mlevel().equals("")?null:Integer.parseInt(item.mlevel()),item.modifiedtime()
        );
    }
}
