package com.studio.crm_system.service;

import com.studio.crm_system.config.SupabaseStorageProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;


public class SupabaseStorageClient {

	private final SupabaseStorageProperties props;
	private final RestClient restClient;

	public SupabaseStorageClient(SupabaseStorageProperties props) {
		this.props = props;
		String base = props.baseUrlNormalized().isEmpty() ? "https://invalid.local" : props.baseUrlNormalized();
		String key = props.getServiceRoleKey() != null ? props.getServiceRoleKey().trim() : "";
		this.restClient = RestClient.builder()
				.baseUrl(base)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
				.defaultHeader("apikey", key)
				.build();
	}

	public void upload(String objectPath, byte[] body, String contentType) {
		if (!props.isConfigured()) throw new IllegalStateException("Supabase Storage не настроен");
		URI uri = objectUri("", normalizeObjectPath(objectPath));
		restClient.post()
				.uri(uri)
				.contentType(MediaType.parseMediaType(contentType))
				.header("x-upsert", "true")
				.body(body)
				.retrieve()
				.toBodilessEntity();
	}

	public byte[] downloadAuthenticated(String objectPath) {
		if (!props.isConfigured()) return null;
		URI uri = objectUri("authenticated/", normalizeObjectPath(objectPath));
		try {
			return restClient.get()
					.uri(uri)
					.retrieve()
					.body(byte[].class);
		} catch (RestClientException e) {
			return null;
		}
	}

	public void delete(String objectPath) {
		if (!props.isConfigured()) return;
		URI uri = objectUri("", normalizeObjectPath(objectPath));
		try {
			restClient.delete()
					.uri(uri)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException e) {
			System.err.println("[SupabaseStorage] Удаление: " + objectPath + " — " + e.getMessage());
		}
	}

	
	private URI objectUri(String authenticatedPrefix, String objectPath) {
		String base = props.baseUrlNormalized();
		String bucket = UriUtils.encodePathSegment(props.getBucket(), StandardCharsets.UTF_8);
		String encodedObject = encodeObjectPath(objectPath);
		String path = "/storage/v1/object/" + authenticatedPrefix + bucket + "/" + encodedObject;
		return URI.create(base + path);
	}

	private static String encodeObjectPath(String objectPath) {
		if (objectPath == null || objectPath.isEmpty()) return "";
		String[] parts = objectPath.split("/");
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) continue;
			if (sb.length() > 0) sb.append('/');
			sb.append(UriUtils.encodePathSegment(part, StandardCharsets.UTF_8));
		}
		return sb.toString();
	}

	private static String normalizeObjectPath(String objectPath) {
		if (objectPath == null) return "";
		String p = objectPath.trim();
		while (p.startsWith("/")) {
			p = p.substring(1);
		}
		return p;
	}
}
