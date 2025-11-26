package com.earseo.core.dto.etl;

public record CommonItemDto(
        String title,
        String addr1,
        String addr2,
        String mapX,
        String mapY,
        String modifiedTime,
        String tel,
        String mLevel,
        String overview
) {}
