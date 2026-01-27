package com.earseo.core.dto.tourApi;

import java.util.List;

public record CommonResponse(Response response){

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
            String overview

    ) {}
}
