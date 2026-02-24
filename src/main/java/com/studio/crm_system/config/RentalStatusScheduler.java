package com.studio.crm_system.config;

import com.studio.crm_system.service.BookingService;
import com.studio.crm_system.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RentalStatusScheduler {

	@Autowired
	private RentalService rentalService;

	@Autowired
	private BookingService bookingService;

	/** Раз в минуту по системным часам (в начале каждой минуты, без рассинхрона). */
	@Scheduled(cron = "0 * * * * ?")
	public void runScheduledTasks() {
		rentalService.updateRentalStatuses();
		bookingService.activateBookings();
		bookingService.deleteExpiredBookings();
	}
}
