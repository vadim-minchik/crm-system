package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Category;
import com.studio.crm_system.entity.PreCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
	List<Category> findByIsDeletedFalse();
	List<Category> findByPreCategoryAndIsDeletedFalse(PreCategory preCategory);
	boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);
	boolean existsByNameIgnoreCaseAndPreCategoryAndIsDeletedFalse(String name, PreCategory preCategory);
	long countByPreCategoryAndIsDeletedFalse(PreCategory preCategory);
}
