package com.studio.crm_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Отдельный конфиг, чтобы не было цикла: SecurityConfig не может объявлять SessionRegistry
 * в том же классе, где он же внедряется в SecurityFilterChain.
 */
@Configuration
public class SecuritySessionBeansConfig {

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}
}
