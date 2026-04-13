package com.studio.crm_system.util;

import com.studio.crm_system.entity.Client;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientPassportChecksTest {

	@Test
	void isPassportValidOn_returnsFalse_whenClientNull() {
		assertFalse(ClientPassportChecks.isPassportValidOn(null, LocalDate.of(2025, 6, 1)));
	}

	@Test
	void isPassportValidOn_returnsFalse_whenOnDateNull() {
		var client = new Client();
		client.setPassportExpiryDate(LocalDate.of(2030, 1, 1));
		assertFalse(ClientPassportChecks.isPassportValidOn(client, null));
	}

	@Test
	void isPassportValidOn_returnsFalse_whenExpiryNull() {
		var client = new Client();
		client.setPassportExpiryDate(null);
		assertFalse(ClientPassportChecks.isPassportValidOn(client, LocalDate.of(2025, 6, 1)));
	}

	@Test
	void isPassportValidOn_returnsTrue_whenExpiryEqualsOnDate() {
		var client = new Client();
		LocalDate expiry = LocalDate.of(2026, 4, 13);
		client.setPassportExpiryDate(expiry);
		assertTrue(ClientPassportChecks.isPassportValidOn(client, expiry));
	}

	@Test
	void isPassportValidOn_returnsTrue_whenExpiryAfterOnDate() {
		var client = new Client();
		client.setPassportExpiryDate(LocalDate.of(2030, 1, 1));
		assertTrue(ClientPassportChecks.isPassportValidOn(client, LocalDate.of(2025, 6, 1)));
	}

	@Test
	void isPassportValidOn_returnsFalse_whenExpiryBeforeOnDate() {
		var client = new Client();
		client.setPassportExpiryDate(LocalDate.of(2020, 1, 1));
		assertFalse(ClientPassportChecks.isPassportValidOn(client, LocalDate.of(2025, 6, 1)));
	}
}
