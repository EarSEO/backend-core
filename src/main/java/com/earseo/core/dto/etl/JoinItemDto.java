package com.earseo.core.dto.etl;

public record JoinItemDto(
        Long id,
        String contentId,
        String title,
        String overview,
        String script
) {
}
