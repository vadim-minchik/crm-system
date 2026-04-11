package com.studio.crm_system.config;

import com.studio.crm_system.service.SupabaseFileStorageService;
import com.studio.crm_system.service.SupabaseStorageClient;
import com.studio.crm_system.service.SupabaseTemplateStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "supabase")
@EnableConfigurationProperties(SupabaseStorageProperties.class)
public class SupabaseStorageBeansConfig {

	private final SupabaseStorageProperties properties;

	public SupabaseStorageBeansConfig(SupabaseStorageProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	public void validate() {
		if (!properties.isConfigured()) {
			StringBuilder hint = new StringBuilder("app.storage.type=supabase: не хватает настроек — ");
			if (properties.getUrl() == null || properties.getUrl().isBlank()) {
				hint.append("supabase.storage.url пустой; ");
			}
			if (properties.getServiceRoleKey() == null || properties.getServiceRoleKey().isBlank()) {
				hint.append("supabase.storage.service-role-key пустой (вставьте ключ JWT/sb_secret после = в application-local.properties "
						+ "или задайте переменную окружения SUPABASE_SERVICE_ROLE_KEY в конфигурации запуска); ");
			}
			if (properties.getBucket() == null || properties.getBucket().isBlank()) {
				hint.append("supabase.storage.bucket пустой; ");
			}
			hint.append("см. application-local.properties.example.");
			throw new IllegalStateException(hint.toString());
		}
	}

	@Bean
	public SupabaseStorageClient supabaseStorageClient() {
		return new SupabaseStorageClient(properties);
	}

	@Bean
	public SupabaseFileStorageService supabaseFileStorageService(SupabaseStorageClient client) {
		return new SupabaseFileStorageService(client);
	}

	@Bean
	public SupabaseTemplateStorageService supabaseTemplateStorageService(SupabaseStorageClient client) {
		return new SupabaseTemplateStorageService(client);
	}
}
