package com.studio.crm_system.config;

import com.studio.crm_system.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Периодически перераспределяет прокаты: ACTIVE → SOON_DEBTOR (приёмка) или DEBTOR (должники) по времени.
 */
@Component
public class RentalStatusScheduler {

	@Autowired
	private RentalService rentalService;

	/** Каждую минуту проверяем, не пора ли перенести прокаты в «Приёмка» или «Должники». */
	@Scheduled(fixedRate = 60_000)
	public void updateRentalStatuses() {
		rentalService.updateRentalStatuses();
	}
}
