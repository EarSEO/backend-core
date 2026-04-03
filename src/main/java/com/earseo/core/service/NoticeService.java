package com.earseo.core.service;

import com.earseo.core.common.exception.BaseException;
import com.earseo.core.common.exception.NoticeError;
import com.earseo.core.dto.etl.NoticeListItem;
import com.earseo.core.dto.request.NoticeCreateRequest;
import com.earseo.core.dto.request.NoticeUpdateRequest;
import com.earseo.core.dto.response.NoticeDeleteResponse;
import com.earseo.core.dto.response.NoticeListResponse;
import com.earseo.core.dto.response.NoticeResponse;
import com.earseo.core.entity.Notice;
import com.earseo.core.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public NoticeResponse getNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BaseException(NoticeError.NOTICE_NOT_FOUND));

        return NoticeResponse.toDto(notice);
    }

    @Transactional(readOnly = true)
    public NoticeListResponse getNoticeList(Pageable pageable) {
        Slice<NoticeListItem> notices = noticeRepository.findNoticeList(pageable);
        return NoticeListResponse.toDto(notices);
    }

    @Transactional
    public NoticeResponse createNotice(NoticeCreateRequest request) {
        Notice notice = Notice.builder()
                .title(request.title())
                .content(request.content())
                .build();

        noticeRepository.save(notice);

        return NoticeResponse.toDto(notice);
    }

    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest noticeUpdateRequest) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BaseException(NoticeError.NOTICE_NOT_FOUND));

        notice.update(
                noticeUpdateRequest.title(), noticeUpdateRequest.content()
        );

        return NoticeResponse.toDto(notice);
    }

    @Transactional
    public NoticeDeleteResponse deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BaseException(NoticeError.NOTICE_NOT_FOUND));

        noticeRepository.delete(notice);

        return new NoticeDeleteResponse(noticeId);
    }
}
