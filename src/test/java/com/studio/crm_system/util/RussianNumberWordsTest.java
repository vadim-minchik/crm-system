package com.studio.crm_system.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RussianNumberWordsTest {

	@Test
	void intToWords_zero() {
		assertEquals("ноль", RussianNumberWords.intToWords(0));
	}

	@Test
	void intToWords_negativeTreatedAsZero() {
		assertEquals("ноль", RussianNumberWords.intToWords(-42));
	}

	@Test
	void intToWords_singleDigit() {
		assertEquals("пять", RussianNumberWords.intToWords(5));
	}

	@Test
	void intToWords_teens() {
		assertEquals("пятнадцать", RussianNumberWords.intToWords(15));
	}

	@Test
	void intToWords_compound() {
		assertEquals("двадцать один", RussianNumberWords.intToWords(21));
	}

	@Test
	void intToWords_thousandsWithFemaleForm() {
		assertEquals("одна тысяча", RussianNumberWords.intToWords(1000));
		assertEquals("две тысячи", RussianNumberWords.intToWords(2000));
	}

	@Test
	void intToWords_aboveMaxReturnsDigits() {
		long n = 1_000_000_000L;
		assertEquals("1000000000", RussianNumberWords.intToWords(n));
	}

	@Test
	void amountIntegerPartWords_nullReturnsDash() {
		assertEquals("—", RussianNumberWords.amountIntegerPartWords(null));
	}

	@Test
	void amountIntegerPartWords_truncatesDown() {
		assertEquals("сто двадцать три", RussianNumberWords.amountIntegerPartWords(new BigDecimal("123.99")));
	}

	@ParameterizedTest
	@CsvSource({
			"1, один",
			"10, десять",
			"100, сто",
			"101, сто один",
			"1000000, один миллион"
	})
	void intToWords_parametrized(long n, String expected) {
		assertEquals(expected, RussianNumberWords.intToWords(n));
	}
}
