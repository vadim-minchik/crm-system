package com.studio.crm_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "supabase.storage")
public class SupabaseStorageProperties {

	
	private String url = "";

	
	private String serviceRoleKey = "";

	
	private String bucket = "crm-uploads";

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getServiceRoleKey() {
		return serviceRoleKey;
	}

	public void setServiceRoleKey(String serviceRoleKey) {
		this.serviceRoleKey = serviceRoleKey;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public boolean isConfigured() {
		return url != null && !url.isBlank()
				&& serviceRoleKey != null && !serviceRoleKey.isBlank()
				&& bucket != null && !bucket.isBlank();
	}

	public String baseUrlNormalized() {
		String u = url == null ? "" : url.trim();
		while (u.endsWith("/")) {
			u = u.substring(0, u.length() - 1);
		}
		return u;
	}
}
