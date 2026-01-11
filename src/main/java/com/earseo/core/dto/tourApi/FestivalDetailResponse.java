package com.earseo.core.dto.tourApi;

import com.earseo.core.dto.etl.DetailItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FestivalDetailResponse(Response response) implements DetailResponse{
    public record Response(
            Body body,
            Header header
    ){}

    public record Header(
            String resultCode,
            String resultMsg
    ) {}

    public record Body(Items items){}

    public record Items(List<Item> item){}

    public record Item(
            String contentid,
            String contenttypeid,
            String usefee,
            @JsonProperty("restdatefestival")
            String restdate,
            @JsonProperty("usetimefestival")
            String usetime,
            @JsonProperty("parkingfestival")
            String parking
    ) {}

    @Override
    public DetailItemDto getDetailItemDto() {
        if (response == null ||
                response.body() == null ||
                response.body().items() == null ||
                response.body().items().item() == null ||
                response.body().items().item().isEmpty()) {

            return null;
        }
        Item item = response.body.items.item.get(0);
        return new DetailItemDto(item.usefee, item.parking, item.restdate, item.usetime);
    }
}
