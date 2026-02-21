package com.studio.crm_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

	private String url;

	private String serviceRoleKey;

	private Storage storage = new Storage();

	public static class Storage {
		private String bucket;

		public String getBucket() { return bucket; }
		public void setBucket(String bucket) { this.bucket = bucket; }
	}

	public String getUrl() { return url; }
	public void setUrl(String url) { this.url = url; }

	public String getServiceRoleKey() { return serviceRoleKey; }
	public void setServiceRoleKey(String serviceRoleKey) { this.serviceRoleKey = serviceRoleKey; }

	public Storage getStorage() { return storage; }
	public void setStorage(Storage storage) { this.storage = storage; }
}
