package com.studio.crm_system.config;

import com.studio.crm_system.service.BookingService;
import com.studio.crm_system.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Периодически: перераспределяет прокаты по статусам; удаляет просроченные брони.
 */
@Component
public class RentalStatusScheduler {

	@Autowired
	private RentalService rentalService;

	@Autowired
	private BookingService bookingService;

	/** Каждую минуту: прокаты в «Приёмка»/«Должники»; брони с прошедшей датой «по какое» удаляются. */
	@Scheduled(fixedRate = 60_000)
	public void runScheduledTasks() {
		rentalService.updateRentalStatuses();
		bookingService.deleteExpiredBookings();
	}
}
