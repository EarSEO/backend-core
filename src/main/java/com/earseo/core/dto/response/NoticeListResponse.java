package com.earseo.core.dto.response;

import com.earseo.core.dto.etl.NoticeListItem;
import org.springframework.data.domain.Slice;

import java.util.List;

public record NoticeListResponse(
    List<NoticeListItem> content,
    int page,
    int size,
    boolean last
) {
    public static NoticeListResponse toDto(Slice<NoticeListItem> page) {
        return new NoticeListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.isLast()
        );
    }
}
