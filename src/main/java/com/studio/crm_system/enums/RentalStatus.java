package com.studio.crm_system.enums;

public enum RentalStatus {
	/** Оформлен, оборудование в резерве; прокат считается начавшимся после отметки «Доставлено». */
	AWAITING_DELIVERY,
	ACTIVE,
	COMPLETED,
	CANCELLED,
	BOOKED,
	DEBTOR,
	SOON_DEBTOR
}
