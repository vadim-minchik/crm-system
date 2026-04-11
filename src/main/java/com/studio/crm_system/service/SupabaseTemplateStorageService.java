package com.studio.crm_system.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;


public class SupabaseTemplateStorageService implements TemplateStorageService {

	public static final String URL_PREFIX = "/storage/documents/templates/";
	private static final String OBJECT_PREFIX = "documents/templates/";

	private final SupabaseStorageClient client;

	public SupabaseTemplateStorageService(SupabaseStorageClient client) {
		this.client = client;
	}

	@Override
	public String uploadTemplate(MultipartFile file, Long templateId) throws IOException {
		String safeName = file.getOriginalFilename() != null
				? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
				: "document.docx";
		if (!safeName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
			safeName = safeName + ".docx";
		}
		String filename = templateId + "_" + safeName;
		String objectKey = OBJECT_PREFIX + filename;
		byte[] bytes = file.getBytes();
		client.upload(objectKey, bytes,
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		return URL_PREFIX + filename;
	}

	@Override
	public byte[] downloadByStoredUrl(String urlOrPath) {
		if (urlOrPath == null || !urlOrPath.startsWith(URL_PREFIX)) return null;
		String filename = urlOrPath.substring(URL_PREFIX.length());
		if (filename.contains("..") || filename.contains("/") || filename.isBlank()) return null;
		return client.downloadAuthenticated(OBJECT_PREFIX + filename);
	}

	@Override
	public void deleteByUrl(String urlOrPath) {
		if (urlOrPath == null || !urlOrPath.startsWith(URL_PREFIX)) return;
		String filename = urlOrPath.substring(URL_PREFIX.length());
		if (filename.contains("..") || filename.contains("/") || filename.isBlank()) return;
		client.delete(OBJECT_PREFIX + filename);
	}
}
