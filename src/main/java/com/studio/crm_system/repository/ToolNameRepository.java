package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Category;
import com.studio.crm_system.entity.ToolName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ToolNameRepository extends JpaRepository<ToolName, Long> {

	List<ToolName> findByIsDeletedFalse();

	List<ToolName> findByCategoryAndIsDeletedFalse(Category category);

	boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

	boolean existsByNameIgnoreCaseAndCategoryAndIsDeletedFalse(String name, Category category);

	long countByCategoryAndIsDeletedFalse(Category category);
}
