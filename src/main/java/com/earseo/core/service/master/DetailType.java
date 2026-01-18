package com.earseo.core.service.master;

import com.earseo.core.dto.tourApi.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum DetailType {
    SIGHT(Set.of("12","76"), SightDetailResponse.class),
    CULTURE(Set.of("14","78"), CultureDetailResponse.class),
    FESTIVAL(Set.of("15","85"), FestivalDetailResponse.class),
    LEPORTS(Set.of("28", "75"), LeportsDetailResponse.class),
    SHOPPING(Set.of("38", "79"), ShoppingDetailResponse.class);

    private final Set<String> id;
    private final Class<? extends DetailResponse> responseType;

    public static DetailType from(String contentTypeId){
        return Arrays.stream(values())
                .filter(t-> t.id.contains(contentTypeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "컨턴츠타입 매칭 에러 : " + contentTypeId));
    }
}
