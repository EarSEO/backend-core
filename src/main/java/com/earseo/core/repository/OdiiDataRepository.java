package com.earseo.core.repository;

import com.earseo.core.dto.etl.JoinItemDto;
import com.earseo.core.entity.OdiiData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OdiiDataRepository extends JpaRepository<OdiiData, Long> {

    @Query(value = """
            SELECT DISTINCT m.id, m.content_id, m.title, MIN(o.script), m.outl
            FROM odii_data o
                     RIGHT JOIN master m ON m.title = o.title
            WHERE m.title IS NOT NULL
            GROUP BY m.id, m.content_id, m.title, m.outl
            ORDER BY m.id
            """, nativeQuery = true)
    List<JoinItemDto> joinWithMaster();
}
