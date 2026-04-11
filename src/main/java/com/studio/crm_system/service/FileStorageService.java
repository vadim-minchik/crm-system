package com.studio.crm_system.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


public interface FileStorageService {

	
	String uploadPassportPhoto(MultipartFile file, Long clientId) throws IOException;

	
	void deleteByUrl(String urlOrPath);
}
