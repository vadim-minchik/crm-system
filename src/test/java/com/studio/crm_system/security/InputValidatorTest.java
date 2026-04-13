package com.studio.crm_system.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidatorTest {

	private InputValidator validator;

	@BeforeEach
	void setUp() {
		validator = new InputValidator();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "ab", "aaaaaaaaaaaaaaaaaaaaa", "user@name", "user name" })
	void isValidLogin_invalid(String login) {
		assertFalse(validator.isValidLogin(login));
	}

	@Test
	void isValidLogin_valid() {
		assertTrue(validator.isValidLogin("vadim123"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "not-an-email", "@nodomain.com" })
	void isValidEmail_invalid(String email) {
		assertFalse(validator.isValidEmail(email));
	}

	@Test
	void isValidEmail_valid() {
		assertTrue(validator.isValidEmail("user.name+tag@example.com"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	void isValidPhone_invalidNullOrEmpty(String phone) {
		assertFalse(validator.isValidPhone(phone));
	}

	@Test
	void isValidPhone_stripsFormatting() {
		assertTrue(validator.isValidPhone("+7 (999) 123-45-67"));
	}

	@Test
	void isValidPhone_tooShort() {
		assertFalse(validator.isValidPhone("12345"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	void isValidCyrillic_invalid(String input) {
		assertFalse(validator.isValidCyrillic(input));
	}

	@Test
	void isValidCyrillic_validAfterCollapsingSpaces() {
		assertTrue(validator.isValidCyrillic("Иван Петров"));
	}

	@Test
	void containsSQLInjection_detectsClassicPatterns() {
		assertTrue(validator.containsSQLInjection("1; DROP TABLE users--"));
		assertTrue(validator.containsSQLInjection("x' OR 1=1--"));
	}

	@Test
	void containsSQLInjection_safeText() {
		assertFalse(validator.containsSQLInjection("Обычный комментарий клиента"));
	}

	@Test
	void containsXSS_detectsScript() {
		assertTrue(validator.containsXSS("<script>alert(1)</script>"));
		assertTrue(validator.containsXSS("<iframe src=x>"));
	}

	@Test
	void containsXSS_nullIsSafe() {
		assertFalse(validator.containsXSS(null));
	}

	@Test
	void sanitize_stripsTagsAndRiskyTokens() {
		String out = validator.sanitize("<b>Hello</b> SELECT * FROM t");
		assertFalse(out.contains("<"));
		assertFalse(out.toLowerCase().contains("select"));
	}

	@Test
	void isSafeInput_nullOrEmpty() {
		assertTrue(validator.isSafeInput(null));
		assertTrue(validator.isSafeInput(""));
	}

	@Test
	void isSafeInput_rejectsInjection() {
		assertFalse(validator.isSafeInput("'; DELETE FROM clients--"));
	}

	@Test
	void cleanPhone_keepsDigitsAndPlus() {
		assertEquals("+79991234567", validator.cleanPhone("+7 (999) 123-45-67"));
	}

	@Test
	void cleanPhone_nullReturnsEmpty() {
		assertEquals("", validator.cleanPhone(null));
	}

	@Test
	void cleanStrict_trimsAndRemovesAllWhitespace() {
		assertEquals("ab", validator.cleanStrict("  a   b  "));
	}

	@Test
	void sanitize_nullReturnsEmpty() {
		assertEquals("", validator.sanitize(null));
	}

	@Test
	void sanitize_whitespaceOnlyReturnsEmpty() {
		assertEquals("", validator.sanitize("   "));
	}
}
