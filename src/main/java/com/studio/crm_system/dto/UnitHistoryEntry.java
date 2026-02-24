package com.studio.crm_system.dto;

import com.studio.crm_system.entity.Booking;
import com.studio.crm_system.entity.Rental;

import java.time.LocalDateTime;

/**
 * Одна запись в истории экземпляра (прокат или бронь) для сортировки и отображения.
 */
public class UnitHistoryEntry {

	public static final String TYPE_RENTAL = "rental";
	public static final String TYPE_BOOKING = "booking";

	private final String type;
	private final Rental rental;
	private final Booking booking;
	private final LocalDateTime sortDate;
	private final boolean active;

	public UnitHistoryEntry(Rental rental, LocalDateTime now) {
		this.type = TYPE_RENTAL;
		this.rental = rental;
		this.booking = null;
		this.sortDate = rental.getDateTo();
		this.active = rental.getStatus() == com.studio.crm_system.enums.RentalStatus.ACTIVE
				&& rental.getDateTo() != null && rental.getDateTo().isAfter(now);
	}

	public UnitHistoryEntry(Booking booking, LocalDateTime now) {
		this.type = TYPE_BOOKING;
		this.rental = null;
		this.booking = booking;
		this.sortDate = booking.getDateTo();
		this.active = booking.getDateTo() != null && booking.getDateTo().isAfter(now)
				&& (booking.getDateFrom() == null || !booking.getDateFrom().isAfter(now));
	}

	public String getType() { return type; }
	public Rental getRental() { return rental; }
	public Booking getBooking() { return booking; }
	public LocalDateTime getSortDate() { return sortDate; }
	public boolean isActive() { return active; }
}
