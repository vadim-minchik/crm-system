package com.studio.crm_system.util;

import java.math.BigDecimal;
import java.math.RoundingMode;


public final class RussianNumberWords {

	private RussianNumberWords() {}

	private static final String[] UNITS_M = {
			"", "один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"
	};
	private static final String[] UNITS_F = {
			"", "одна", "две", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"
	};
	private static final String[] TEENS = {
			"десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать",
			"пятнадцать", "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать"
	};
	private static final String[] TENS = {
			"", "", "двадцать", "тридцать", "сорок", "пятьдесят",
			"шестьдесят", "семьдесят", "восемьдесят", "девяносто"
	};
	private static final String[] HUNDREDS = {
			"", "сто", "двести", "триста", "четыреста", "пятьсот",
			"шестьсот", "семьсот", "восемьсот", "девятьсот"
	};

	
	public static String intToWords(long n) {
		if (n < 0) n = 0;
		if (n > 999_999_999L) return String.valueOf(n);
		if (n == 0) return "ноль";

		int millions = (int) (n / 1_000_000);
		int thousands = (int) ((n % 1_000_000) / 1000);
		int rest = (int) (n % 1000);

		StringBuilder sb = new StringBuilder();
		if (millions > 0) {
			sb.append(hundredsToWords(millions, false)).append(" ").append(millionForm(millions)).append(" ");
		}
		if (thousands > 0) {
			sb.append(hundredsToWords(thousands, true)).append(" ").append(thousandForm(thousands)).append(" ");
		}
		if (rest > 0) {
			sb.append(hundredsToWords(rest, false));
		}
		return sb.toString().trim().replaceAll("\\s+", " ");
	}

	
	public static String amountIntegerPartWords(BigDecimal amount) {
		if (amount == null) return "—";
		long whole = amount.setScale(0, RoundingMode.DOWN).longValue();
		return intToWords(whole);
	}

	
	public static String daysCountWords(long days) {
		return intToWords(days);
	}

	
	private static String hundredsToWords(int n, boolean femaleOneTwo) {
		if (n < 0 || n > 999) return String.valueOf(n);
		int h = n / 100;
		int mod100 = n % 100;
		StringBuilder sb = new StringBuilder();
		if (h > 0) sb.append(HUNDREDS[h]).append(" ");
		if (mod100 >= 10 && mod100 <= 19) {
			sb.append(TEENS[mod100 - 10]);
		} else {
			int t = mod100 / 10;
			int u = mod100 % 10;
			if (t >= 2) sb.append(TENS[t]).append(" ");
			if (u > 0) {
				if (femaleOneTwo && u <= 2) sb.append(UNITS_F[u]);
				else sb.append(UNITS_M[u]);
			}
		}
		return sb.toString().trim();
	}

	private static String millionForm(int n) {
		int mod100 = n % 100;
		int mod10 = n % 10;
		if (mod100 >= 11 && mod100 <= 14) return "миллионов";
		if (mod10 == 1) return "миллион";
		if (mod10 >= 2 && mod10 <= 4) return "миллиона";
		return "миллионов";
	}

	private static String thousandForm(int n) {
		int mod100 = n % 100;
		int mod10 = n % 10;
		if (mod100 >= 11 && mod100 <= 14) return "тысяч";
		if (mod10 == 1) return "тысяча";
		if (mod10 >= 2 && mod10 <= 4) return "тысячи";
		return "тысяч";
	}
}
