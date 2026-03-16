package com.studio.crm_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Отдаёт загруженные файлы (фото клиентов) по URL /uploads/** с диска сервера.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Value("${file.upload-dir:uploads}")
	private String uploadDir;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String path = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
		// Только фото клиентов — шаблоны отдаются через /documents/{id}/file с авторизацией
		registry.addResourceHandler("/uploads/clients/**")
				.addResourceLocations("file:" + path + "clients/");
	}
}
