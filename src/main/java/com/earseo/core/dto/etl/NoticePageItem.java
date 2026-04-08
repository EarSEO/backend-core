package com.earseo.core.dto.etl;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record NoticePageItem(
        Long noticeId,
        String title,
        @JsonFormat(pattern = "yyyy/MM/dd")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy/MM/dd")
        LocalDateTime updatedAt
) {
}
