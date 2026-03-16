package com.studio.crm_system.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Хранилище файлов шаблонов документов (.docx).
 * Реализации: локальный диск ({@link LocalTemplateStorageService}) или Supabase ({@link SupabaseStorageService}).
 */
public interface TemplateStorageService {

	/**
	 * Загружает файл шаблона. Возвращает URL или путь для сохранения в БД.
	 */
	String uploadTemplate(MultipartFile file, Long templateId) throws IOException;

	/**
	 * Скачивает файл по сохранённому URL/пути (для отдачи через контроллер).
	 */
	byte[] downloadByStoredUrl(String urlOrPath);

	/**
	 * Удаляет файл по сохранённому URL/пути.
	 */
	void deleteByUrl(String urlOrPath);
}
