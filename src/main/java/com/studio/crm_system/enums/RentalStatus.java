package com.studio.crm_system.enums;

/**
 * Статус проката.
 */
public enum RentalStatus {
	ACTIVE,      // В прокате — оборудование у клиента (Equipment = BUSY)
	COMPLETED,   // Завершён — оборудование возвращено (Equipment = FREE)
	CANCELLED,   // Отменён
	BOOKED,      // Забронировано (на будущее)
	DEBTOR,      // Должник — срок возврата прошёл, клиент не вернул
	SOON_DEBTOR  // Приёмка — скоро срок возврата, пора забирать
}
