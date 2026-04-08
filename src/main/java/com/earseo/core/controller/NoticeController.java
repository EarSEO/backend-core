package com.earseo.core.controller;

import com.earseo.core.common.BaseResponse;
import com.earseo.core.dto.request.NoticeCreateRequest;
import com.earseo.core.dto.request.NoticeUpdateRequest;
import com.earseo.core.dto.request.PageableRequest;
import com.earseo.core.dto.response.NoticeDeleteResponse;
import com.earseo.core.dto.response.NoticePageResponse;
import com.earseo.core.dto.response.NoticeResponse;
import com.earseo.core.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
    public ResponseEntity<BaseResponse<NoticePageResponse>> getNoticeList(
            @ParameterObject
            PageableRequest pageableRequest
    ) {
        return ResponseEntity.ok(BaseResponse.ok(noticeService.getNoticeList(pageableRequest.toPageable())));
    }

    @PostMapping("/api/admin/core/notice")
    public ResponseEntity<BaseResponse<NoticeResponse>> createNotice(
            @RequestBody @Valid NoticeCreateRequest request
    ){
        return ResponseEntity.ok(BaseResponse.ok(noticeService.createNotice(request)));
    }

    @PutMapping("/api/admin/core/notice/{noticeId}")
    public ResponseEntity<BaseResponse<NoticeResponse>> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody @Valid NoticeUpdateRequest request
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
