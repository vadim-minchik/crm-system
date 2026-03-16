package com.studio.crm_system.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Абстракция хранилища для фото паспорта клиента.
 * Реализации: локальный диск сервера ({@link LocalFileStorageService}) или Supabase ({@link SupabaseStorageService}).
 */
public interface FileStorageService {

	/**
	 * Загружает фото паспорта, при необходимости сжимает. Возвращает URL или путь для отображения.
	 */
	String uploadPassportPhoto(MultipartFile file, Long clientId) throws IOException;

	/**
	 * Удаляет файл по ранее сохранённому URL/пути.
	 */
	void deleteByUrl(String urlOrPath);
}
