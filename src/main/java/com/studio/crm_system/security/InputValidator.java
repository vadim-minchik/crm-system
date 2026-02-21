package com.studio.crm_system.security;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class InputValidator {

	private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
	private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
	private static final Pattern CYRILLIC_PATTERN = Pattern.compile("^[А-Яа-яЁё]+$");
	private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("('.*(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|DECLARE)\\b).*)|(--|;|/\\*|\\*/|xp_|sp_)", 			Pattern.CASE_INSENSITIVE);
	private static final Pattern XSS_PATTERN = Pattern.compile("<script|<iframe|javascript:|onerror=|onload=", Pattern.CASE_INSENSITIVE);

	public boolean isValidLogin(String login) {
		if (login == null || login.isEmpty() || login.length() < 3 || login.length() > 20) {
			return false;
		}
		return LOGIN_PATTERN.matcher(login).matches();
	}

	public boolean isValidEmail(String email) {
		if (email == null || email.isEmpty()) {
			return false;
		}
		return EMAIL_PATTERN.matcher(email).matches();
	}

	public boolean isValidPhone(String phone) {
		if (phone == null || phone.isEmpty()) {
			return false;
		}
		String cleaned = phone.replaceAll("[\\s-()]", "");
		return PHONE_PATTERN.matcher(cleaned).matches();
	}

	public boolean isValidCyrillic(String input) {
		if (input == null || input.isEmpty()) {
			return false;
		}
		String cleaned = input.trim().replaceAll("\\s+", "");
		return CYRILLIC_PATTERN.matcher(cleaned).matches();
	}

	public boolean containsSQLInjection(String input) {
		if (input == null) {
			return false;
		}
		return SQL_INJECTION_PATTERN.matcher(input).find();
	}

	public boolean containsXSS(String input) {
		if (input == null) {
			return false;
		}
		return XSS_PATTERN.matcher(input).find();
	}

	public String sanitize(String input) {
		if (input == null) {
			return "";
		}
		String cleaned = input.replaceAll("<[^>]*>", "");
		cleaned = cleaned.replaceAll("(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION)", "");
		cleaned = cleaned.replaceAll("[<>\"'%;()&+]", "");
		cleaned = cleaned.trim().replaceAll("\\s+", " ");
		return cleaned;
	}

	public boolean isSafeInput(String input) {
		if (input == null || input.isEmpty()) {
			return true;
		}
		if (containsSQLInjection(input)) {
			return false;
		}
		if (containsXSS(input)) {
			return false;
		}
		return true;
	}

	public String cleanPhone(String phone) {
		if (phone == null) {
			return "";
		}
		return phone.replaceAll("[^0-9+]", "");
	}

	public String cleanStrict(String input) {
		if (input == null) {
			return "";
		}
		return input.trim().replaceAll("\\s+", "");
	}
}
