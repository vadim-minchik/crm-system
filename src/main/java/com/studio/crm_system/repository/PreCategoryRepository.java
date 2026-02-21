package com.studio.crm_system.repository;

import com.studio.crm_system.entity.PreCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PreCategoryRepository extends JpaRepository<PreCategory, Long> {
	List<PreCategory> findByIsDeletedFalse();
	boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);
}
