package com.studio.crm_system.service;

import com.studio.crm_system.storage.PassportImageCompressor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;


@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

	private static final String URL_PREFIX = "/uploads/";

	@Value("${file.upload-dir:uploads}")
	private String uploadDir;

	@Override
	public String uploadPassportPhoto(MultipartFile file, Long clientId) throws IOException {
		byte[] imageBytes = PassportImageCompressor.compress(file);
		Path base = Paths.get(uploadDir).toAbsolutePath();
		Path clientsDir = base.resolve("clients");
		Files.createDirectories(clientsDir);

		String filename = clientId + "_" + UUID.randomUUID() + ".jpg";
		Path target = clientsDir.resolve(filename);
		Files.copy(new ByteArrayInputStream(imageBytes), target, StandardCopyOption.REPLACE_EXISTING);

		return URL_PREFIX + "clients/" + filename;
	}

	@Override
	public void deleteByUrl(String urlOrPath) {
		if (urlOrPath == null || !urlOrPath.startsWith(URL_PREFIX)) return;
		String relative = urlOrPath.substring(URL_PREFIX.length());
		Path full = Paths.get(uploadDir).toAbsolutePath().resolve(relative);
		try {
			Files.deleteIfExists(full);
		} catch (IOException e) {
			System.err.println("[LocalStorage] Не удалось удалить файл: " + full + " — " + e.getMessage());
		}
	}
}
