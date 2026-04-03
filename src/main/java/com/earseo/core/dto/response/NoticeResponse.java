package com.earseo.core.dto.response;

import com.earseo.core.entity.Notice;

import java.time.format.DateTimeFormatter;

public record NoticeResponse(
        Long noticeId,
        String noticeTitle,
        String noticeContent,
        String createdAt,
        String updatedAt
) {
    public static NoticeResponse toDto(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                notice.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        );
    }
}
