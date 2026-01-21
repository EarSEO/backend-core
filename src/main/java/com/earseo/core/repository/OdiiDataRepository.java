package com.earseo.core.repository;

import com.earseo.core.dto.etl.JoinItemDto;
import com.earseo.core.entity.OdiiData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OdiiDataRepository extends JpaRepository<OdiiData, Long> {

    @Query(value = """
            SELECT DISTINCT km.id, km.content_id, km.title, MIN(o.script), km.overview
            FROM odii_data o
                     RIGHT JOIN ko_master km ON km.title = o.title
            WHERE km.title IS NOT NULL
            GROUP BY km.id, km.content_id, km.title, km.overview
            ORDER BY km.id
            """, nativeQuery = true)
    List<JoinItemDto> joinWithMasterKo();

    @Query(value = """
            SELECT DISTINCT em.id, em.content_id, em.title, MIN(o.script), em.overview
            FROM odii_data o
                     RIGHT JOIN en_master em ON em.title = o.title
            WHERE em.title IS NOT NULL
            GROUP BY em.id, em.content_id, em.title, em.overview
            ORDER BY em.id
            """, nativeQuery = true)
    List<JoinItemDto> joinWithMasterEn();
}
