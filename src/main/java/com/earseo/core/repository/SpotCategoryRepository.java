package com.earseo.core.repository;

import com.earseo.core.entity.SpotCategory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpotCategoryRepository extends CrudRepository<SpotCategory, Long> {
}
