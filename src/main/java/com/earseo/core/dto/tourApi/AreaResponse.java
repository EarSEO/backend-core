package com.earseo.core.dto.tourApi;

import java.util.List;

public record AreaResponse(Response response) {

    public record Response(
            Body body,
            Header header
    ){}

    public record Header(
            String resultCode,
            String resultMsg
    ) {}

    public record Body(Items items, int numOfRows){}

    public record Items(List<Item> item){}

    public record Item(
            String contentid,
            String contenttypeid,
            String title,

            String addr1,
            String addr2,

            String cat1,
            String cat2,
            String cat3,

            String mapx,
            String mapy,
            String mlevel,

            String firstimage,
            String firstimage2,

            String tel,

            String modifiedtime

    ) {}
}
