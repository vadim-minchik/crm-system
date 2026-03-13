package com.studio.crm_system.repository;

import com.studio.crm_system.entity.DocumentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {

	List<DocumentTemplate> findAllByOrderByCreatedAtDesc();
}
