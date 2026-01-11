package com.earseo.core.service.master;

import com.earseo.core.dto.tourApi.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum DetailType {
    SIGHT("12", SightDetailResponse.class),
    CULTURE("14", CultureDetailResponse.class),
    FESTIVAL("15", FestivalDetailResponse.class),
    LEPORTS("28", LeportsDetailResponse.class),
    SHOPPING("38", ShoppingDetailResponse.class);

    private final String id;
    private final Class<? extends DetailResponse> responseType;

    public static DetailType from(String contentTypeId){
        return Arrays.stream(values())
                .filter(t-> t.id.equals(contentTypeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "컨턴츠타입 매칭 에러 : " + contentTypeId));
    }
}
