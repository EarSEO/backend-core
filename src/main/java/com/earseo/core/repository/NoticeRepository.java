package com.earseo.core.repository;

import com.earseo.core.dto.etl.NoticeListItem;
import com.earseo.core.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("SELECT new com.earseo.core.dto.etl.NoticeListItem(n.id, n.title) FROM Notice n")
    Slice<NoticeListItem> findNoticeList(Pageable pageable);
}
