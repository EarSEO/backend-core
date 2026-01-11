package com.earseo.core.repository;

import com.earseo.core.entity.EnMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnMasterRepository extends JpaRepository<EnMaster, Long> {
}
