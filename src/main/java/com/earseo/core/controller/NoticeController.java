package com.earseo.core.controller;

import com.earseo.core.common.BaseResponse;
import com.earseo.core.dto.request.NoticeCreateRequest;
import com.earseo.core.dto.request.NoticeUpdateRequest;
import com.earseo.core.dto.response.NoticeDeleteResponse;
import com.earseo.core.dto.response.NoticeListResponse;
import com.earseo.core.dto.response.NoticeResponse;
import com.earseo.core.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/api/core/notice/{noticeId}")
    public ResponseEntity<BaseResponse<NoticeResponse>> getNotice(
            @PathVariable Long noticeId
    ) {
        return ResponseEntity.ok(BaseResponse.ok(noticeService.getNotice(noticeId)));
    }

    @GetMapping("/api/core/notice")
    public ResponseEntity<BaseResponse<NoticeListResponse>> getNoticeList(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(BaseResponse.ok(noticeService.getNoticeList(pageable)));
    }

    @PostMapping("/api/admin/core/notice")
    public ResponseEntity<BaseResponse<NoticeResponse>> createNotice(
            @RequestBody NoticeCreateRequest request
    ){
        return ResponseEntity.ok(BaseResponse.ok(noticeService.createNotice(request)));
    }

    @PutMapping("/api/admin/core/notice/{noticeId}")
    public ResponseEntity<BaseResponse<NoticeResponse>> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody NoticeUpdateRequest request
    ){
        return ResponseEntity.ok(BaseResponse.ok(noticeService.updateNotice(noticeId, request)));
    }

    @DeleteMapping("/api/admin/core/notice/{noticeId}")
    public ResponseEntity<BaseResponse<NoticeDeleteResponse>> deleteNotice(
            @PathVariable Long noticeId
    ){
        return ResponseEntity.ok(BaseResponse.ok(noticeService.deleteNotice(noticeId)));
    }
}
