package com.studio.crm_system.service;

import com.studio.crm_system.storage.PassportImageCompressor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;


public class SupabaseFileStorageService implements FileStorageService {

	public static final String URL_PREFIX = "/storage/clients/";
	private static final String OBJECT_PREFIX = "clients/";

	private final SupabaseStorageClient client;

	public SupabaseFileStorageService(SupabaseStorageClient client) {
		this.client = client;
	}

	@Override
	public String uploadPassportPhoto(MultipartFile file, Long clientId) throws IOException {
		byte[] imageBytes = PassportImageCompressor.compress(file);
		String filename = clientId + "_" + UUID.randomUUID() + ".jpg";
		String objectKey = OBJECT_PREFIX + filename;
		client.upload(objectKey, imageBytes, "image/jpeg");
		return URL_PREFIX + filename;
	}

	@Override
	public void deleteByUrl(String urlOrPath) {
		if (urlOrPath == null || !urlOrPath.startsWith(URL_PREFIX)) return;
		String filename = urlOrPath.substring(URL_PREFIX.length());
		if (filename.contains("..") || filename.contains("/") || filename.isBlank()) return;
		client.delete(OBJECT_PREFIX + filename);
	}

	public byte[] downloadPassportFile(String filename) {
		if (filename == null || filename.isBlank() || filename.contains("..") || filename.contains("/")) {
			return null;
		}
		return client.downloadAuthenticated(OBJECT_PREFIX + filename);
	}
}
