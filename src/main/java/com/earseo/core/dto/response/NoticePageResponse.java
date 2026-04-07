package com.earseo.core.dto.response;

import com.earseo.core.dto.etl.NoticePageItem;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Slice;

import java.util.List;

public record NoticePageResponse(
    @Schema(description = "공지사항 목록 (페이징/정렬 반영)")
    List<NoticePageItem> content,
    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    int number,
    @Schema(description = "페이지 크기", example = "10")
    int size,
    @Schema(description = "첫 페이지 여부", example = "true")
    boolean isFirst,
    @Schema(description = "마지막 페이지 여부", example = "false")
    boolean isLast,
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext,
    @Schema(description = "이전 페이지 존재 여부", example = "false")
    boolean hasPrevious
) {
    public static NoticePageResponse toDto(Slice<NoticePageItem> page) {
        return new NoticePageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
