package com.studio.crm_system.util;

import com.studio.crm_system.entity.Client;

import java.time.LocalDate;

/**
 * Проверка срока действия паспорта клиента (дата «действителен до» включительно).
 */
public final class ClientPassportChecks {

	private ClientPassportChecks() {}

	/**
	 * Паспорт действителен на указанную календарную дату, если срок окончания не раньше этой даты.
	 */
	public static boolean isPassportValidOn(Client client, LocalDate onDate) {
		if (client == null || onDate == null) {
			return false;
		}
		LocalDate expiry = client.getPassportExpiryDate();
		if (expiry == null) {
			return false;
		}
		return !expiry.isBefore(onDate);
	}

	/** Удобно для выдачи документа «сегодня». */
	public static boolean isPassportValidToday(Client client) {
		return isPassportValidOn(client, LocalDate.now());
	}
}
