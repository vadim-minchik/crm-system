package com.studio.crm_system.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


public interface TemplateStorageService {

	
	String uploadTemplate(MultipartFile file, Long templateId) throws IOException;

	
	byte[] downloadByStoredUrl(String urlOrPath);

	
	void deleteByUrl(String urlOrPath);
}
