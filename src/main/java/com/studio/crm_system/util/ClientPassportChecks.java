package com.studio.crm_system.util;

import com.studio.crm_system.entity.Client;

import java.time.LocalDate;


public final class ClientPassportChecks {

	private ClientPassportChecks() {}

	
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

	
	public static boolean isPassportValidToday(Client client) {
		return isPassportValidOn(client, LocalDate.now());
	}
}
