package com.studio.crm_system.controller;

import com.studio.crm_system.service.CrmDataRevisionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DataRevisionApiController {

	private final CrmDataRevisionService revisionService;

	public DataRevisionApiController(CrmDataRevisionService revisionService) {
		this.revisionService = revisionService;
	}

	@GetMapping(value = "/api/data-revision", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Long> dataRevision() {
		return Map.of("revision", revisionService.getRevision());
	}
}
