package com.studio.crm_system.config;

import com.studio.crm_system.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Выбор реализации хранилища: локальный диск (app.storage.type=local) или Supabase.
 * Фото клиентов и шаблоны документов используют один и тот же тип.
 */
@Configuration
public class StorageConfig {

	@Value("${app.storage.type:local}")
	private String storageType;

	@Bean
	@Primary
	public FileStorageService fileStorageService(
			LocalFileStorageService localFileStorageService,
			SupabaseStorageService supabaseStorageService) {
		if ("supabase".equalsIgnoreCase(storageType)) {
			return supabaseStorageService;
		}
		return localFileStorageService;
	}

	@Bean
	@Primary
	public TemplateStorageService templateStorageService(
			LocalTemplateStorageService localTemplateStorageService,
			SupabaseStorageService supabaseStorageService) {
		if ("supabase".equalsIgnoreCase(storageType)) {
			return supabaseStorageService;
		}
		return localTemplateStorageService;
	}
}
