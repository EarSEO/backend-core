package com.earseo.core.repository;

import com.earseo.core.entity.KoMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KoMasterRepository extends JpaRepository<KoMaster, Long> {
}
