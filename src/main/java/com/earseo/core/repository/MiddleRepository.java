package com.earseo.core.repository;

import com.earseo.core.entity.MiddleData;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MiddleRepository extends JpaRepository<MiddleData, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM MiddleData m WHERE m.contentTypeId = :contentTypeId")
    int deleteByContentType(@Param("contentTypeId") String contentTypeId);
}
