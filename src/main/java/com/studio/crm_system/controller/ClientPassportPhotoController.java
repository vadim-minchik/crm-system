package com.studio.crm_system.controller;

import com.studio.crm_system.service.SupabaseFileStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.TimeUnit;


@Controller
@ConditionalOnBean(SupabaseFileStorageService.class)
public class ClientPassportPhotoController {

	private final SupabaseFileStorageService fileStorageService;

	public ClientPassportPhotoController(SupabaseFileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	@GetMapping("/storage/clients/{fileName:.+}")
	public ResponseEntity<byte[]> getPhoto(@PathVariable String fileName) {
		byte[] body = fileStorageService.downloadPassportFile(fileName);
		if (body == null || body.length == 0) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok()
				.cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
				.header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
				.body(body);
	}
}
