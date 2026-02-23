package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Category;
import com.studio.crm_system.entity.PreCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
	List<Category> findByIsDeletedFalse();
	List<Category> findByPreCategoryAndIsDeletedFalse(PreCategory preCategory);
	List<Category> findByPreCategoryAndParentCategoryIsNullAndIsDeletedFalse(PreCategory preCategory);
	List<Category> findByParentCategoryAndIsDeletedFalse(Category parent);
	boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);
	boolean existsByNameIgnoreCaseAndPreCategoryAndIsDeletedFalse(String name, PreCategory preCategory);
	boolean existsByNameIgnoreCaseAndPreCategoryAndParentCategoryIsNullAndIsDeletedFalse(String name, PreCategory preCategory);
	boolean existsByNameIgnoreCaseAndParentCategoryAndIsDeletedFalse(String name, Category parent);
	long countByPreCategoryAndIsDeletedFalse(PreCategory preCategory);
	long countByPreCategoryAndParentCategoryIsNullAndIsDeletedFalse(PreCategory preCategory);
	long countByParentCategoryAndIsDeletedFalse(Category parent);
}
