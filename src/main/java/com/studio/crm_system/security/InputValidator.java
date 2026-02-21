package com.studio.crm_system.security;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

/**
 * Валидатор входных данных для защиты от инъекций и XSS
 */
@Component
public class InputValidator {

	// Паттерны для валидации
	private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
	private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
	private static final Pattern CYRILLIC_PATTERN = Pattern.compile("^[А-Яа-яЁё]+$");
	
	// Опасные символы для SQL инъекций
	private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("('.*(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|DECLARE)\\b).*)|(--|;|/\\*|\\*/|xp_|sp_)", Pattern.CASE_INSENSITIVE);
	
	// Опасные символы для XSS
	private static final Pattern XSS_PATTERN = Pattern.compile("<script|<iframe|javascript:|onerror=|onload=", Pattern.CASE_INSENSITIVE);

	/**
	 * Проверка логина (только a-z, A-Z, 0-9)
	 */
	public boolean isValidLogin(String login) {
		if (login == null || login.isEmpty() || login.length() < 3 || login.length() > 20) {
			return false;
		}
		return LOGIN_PATTERN.matcher(login).matches();
	}

	/**
	 * Проверка email
	 */
	public boolean isValidEmail(String email) {
		if (email == null || email.isEmpty()) {
			return false;
		}
		return EMAIL_PATTERN.matcher(email).matches();
	}

	/**
	 * Проверка телефона
	 */
	public boolean isValidPhone(String phone) {
		if (phone == null || phone.isEmpty()) {
			return false;
		}
		String cleaned = phone.replaceAll("[\\s-()]", "");
		return PHONE_PATTERN.matcher(cleaned).matches();
	}

	/**
	 * Проверка на русские буквы (для ФИО)
	 */
	public boolean isValidCyrillic(String input) {
		if (input == null || input.isEmpty()) {
			return false;
		}
		String cleaned = input.trim().replaceAll("\\s+", "");
		return CYRILLIC_PATTERN.matcher(cleaned).matches();
	}

	/**
	 * Защита от SQL инъекций
	 */
	public boolean containsSQLInjection(String input) {
		if (input == null) {
			return false;
		}
		return SQL_INJECTION_PATTERN.matcher(input).find();
	}

	/**
	 * Защита от XSS
	 */
	public boolean containsXSS(String input) {
		if (input == null) {
			return false;
		}
		return XSS_PATTERN.matcher(input).find();
	}

	/**
	 * Очистка строки от опасных символов
	 */
	public String sanitize(String input) {
		if (input == null) {
			return "";
		}
		
		// Удаляем HTML теги
		String cleaned = input.replaceAll("<[^>]*>", "");
		
		// Удаляем SQL ключевые слова
		cleaned = cleaned.replaceAll("(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION)", "");
		
		// Удаляем опасные символы
		cleaned = cleaned.replaceAll("[<>\"'%;()&+]", "");
		
		// Trim и удаление множественных пробелов
		cleaned = cleaned.trim().replaceAll("\\s+", " ");
		
		return cleaned;
	}

	/**
	 * Валидация любой строки (основной метод)
	 */
	public boolean isSafeInput(String input) {
		if (input == null || input.isEmpty()) {
			return true; // Пустая строка безопасна
		}
		
		// Проверяем на SQL инъекции
		if (containsSQLInjection(input)) {
			return false;
		}
		
		// Проверяем на XSS
		if (containsXSS(input)) {
			return false;
		}
		
		return true;
	}

	/**
	 * Очистка номера телефона
	 */
	public String cleanPhone(String phone) {
		if (phone == null) {
			return "";
		}
		return phone.replaceAll("[^0-9+]", "");
	}

	/**
	 * Очистка от пробелов
	 */
	public String cleanStrict(String input) {
		if (input == null) {
			return "";
		}
		return input.trim().replaceAll("\\s+", "");
	}
}
