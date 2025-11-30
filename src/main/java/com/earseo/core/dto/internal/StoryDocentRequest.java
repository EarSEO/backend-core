package com.earseo.core.dto.internal;

public record StoryDocentRequest(
        Long storySpotId,
        Long summaryId,
        String summary,
        String locale
) {
}
