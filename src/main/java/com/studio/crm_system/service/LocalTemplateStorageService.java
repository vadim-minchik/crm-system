package com.studio.crm_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;


@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalTemplateStorageService implements TemplateStorageService {

	private static final String URL_PREFIX = "/uploads/";

	@Value("${file.upload-dir:uploads}")
	private String uploadDir;

	@Override
	public String uploadTemplate(MultipartFile file, Long templateId) throws IOException {
		String safeName = file.getOriginalFilename() != null
				? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
				: "document.docx";
		if (!safeName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
			safeName = safeName + ".docx";
		}
		Path base = Paths.get(uploadDir).toAbsolutePath();
		Path templatesDir = base.resolve("documents").resolve("templates");
		Files.createDirectories(templatesDir);

		String filename = templateId + "_" + safeName;
		Path target = templatesDir.resolve(filename);
		Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

		return URL_PREFIX + "documents/templates/" + filename;
	}

	@Override
	public byte[] downloadByStoredUrl(String urlOrPath) {
		if (urlOrPath == null || !urlOrPath.startsWith(URL_PREFIX)) return null;
		String relative = urlOrPath.substring(URL_PREFIX.length());
		Path full = Paths.get(uploadDir).toAbsolutePath().resolve(relative);
		try {
			return Files.readAllBytes(full);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public void deleteByUrl(String urlOrPath) {
		if (urlOrPath == null || !urlOrPath.startsWith(URL_PREFIX)) return;
		String relative = urlOrPath.substring(URL_PREFIX.length());
		Path full = Paths.get(uploadDir).toAbsolutePath().resolve(relative);
		try {
			Files.deleteIfExists(full);
		} catch (IOException e) {
			System.err.println("[LocalTemplateStorage] Не удалось удалить файл: " + full + " — " + e.getMessage());
		}
	}
}
