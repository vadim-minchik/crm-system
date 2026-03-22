package com.studio.crm_system.config;

import com.studio.crm_system.service.CrmDataRevisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Добавляет в модель номер ревизии данных для встраивания в HTML и сравнения с {@code /api/data-revision}.
 */
@ControllerAdvice
public class CrmDataRevisionModelAdvice {

	@Autowired
	private CrmDataRevisionService revisionService;

	@ModelAttribute("dataRevision")
	public long dataRevision() {
		return revisionService.getRevision();
	}
}
