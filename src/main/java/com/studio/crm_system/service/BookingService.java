package com.studio.crm_system.service;

import com.studio.crm_system.entity.Booking;
import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.enums.EquipmentStatus;
import com.studio.crm_system.repository.BookingRepository;
import com.studio.crm_system.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

	@Autowired
	private BookingRepository bookingRepository;

	@Autowired
	private EquipmentRepository equipmentRepository;

	public List<Booking> findAll() {
		return bookingRepository.findAllByOrderByCreatedAtDesc();
	}

	public Optional<Booking> findById(Long id) {
		return bookingRepository.findById(id);
	}

	/** Только свободное оборудование (для выбора в форме брони). */
	public List<Equipment> findFreeEquipment() {
		return equipmentRepository.findByStatusAndIsDeletedFalse(EquipmentStatus.FREE);
	}

	private static final int MAX_EQUIPMENT_PER_BOOKING = 50;

	/** Одна бронь: один инструмент. */
	@Transactional
	public String create(String phoneNumber, Long equipmentId, LocalDateTime dateTo, String comment) {
		if (phoneNumber == null || phoneNumber.isBlank()) return "phone_required";
		if (equipmentId == null) return "equipment_required";
		if (dateTo == null) return "date_to_required";
		if (!dateTo.isAfter(LocalDateTime.now())) return "date_to_in_past";

		Equipment equipment = equipmentRepository.findByIdAndIsDeletedFalse(equipmentId).orElse(null);
		if (equipment == null) return "equipment_not_found";
		if (equipment.getStatus() != EquipmentStatus.FREE) return "equipment_not_free";

		Booking booking = new Booking();
		booking.setPhoneNumber(phoneNumber.trim());
		booking.setEquipment(equipment);
		booking.setDateTo(dateTo);
		if (comment != null && !comment.isBlank()) booking.setComment(comment.trim());
		bookingRepository.save(booking);
		equipment.setStatus(EquipmentStatus.RESERVED);
		equipmentRepository.save(equipment);
		return null;
	}

	/** Несколько броней: по одной на каждый выбранный инструмент (1–50). */
	@Transactional
	public String createBatch(String phoneNumber, List<Long> equipmentIds, LocalDateTime dateTo, String comment) {
		if (phoneNumber == null || phoneNumber.isBlank()) return "phone_required";
		if (equipmentIds == null || equipmentIds.isEmpty()) return "equipment_required";
		if (equipmentIds.size() > MAX_EQUIPMENT_PER_BOOKING) return "too_many_equipment";
		if (dateTo == null) return "date_to_required";
		if (!dateTo.isAfter(LocalDateTime.now())) return "date_to_in_past";

		List<Equipment> toReserve = new ArrayList<>();
		for (Long eid : equipmentIds) {
			Equipment equipment = equipmentRepository.findByIdAndIsDeletedFalse(eid).orElse(null);
			if (equipment == null) return "equipment_not_found";
			if (equipment.getStatus() != EquipmentStatus.FREE) return "equipment_not_free";
			toReserve.add(equipment);
		}

		for (Equipment equipment : toReserve) {
			Booking booking = new Booking();
			booking.setPhoneNumber(phoneNumber.trim());
			booking.setEquipment(equipment);
			booking.setDateTo(dateTo);
			if (comment != null && !comment.isBlank()) booking.setComment(comment.trim());
			bookingRepository.save(booking);
			equipment.setStatus(EquipmentStatus.RESERVED);
			equipmentRepository.save(equipment);
		}
		return null;
	}

	/** Удалить бронь: один инструмент снова FREE. */
	@Transactional
	public String delete(Long id) {
		Booking booking = bookingRepository.findById(id).orElse(null);
		if (booking == null) return "not_found";

		Equipment eq = booking.getEquipment();
		if (eq != null) {
			eq.setStatus(EquipmentStatus.FREE);
			equipmentRepository.save(eq);
		}
		bookingRepository.delete(booking);
		return null;
	}

	/** Удалить просроченные брони: по каждой — освободить один инструмент. */
	@Transactional
	public void deleteExpiredBookings() {
		LocalDateTime now = LocalDateTime.now();
		for (Booking b : bookingRepository.findByDateToLessThanEqual(now)) {
			Equipment eq = b.getEquipment();
			if (eq != null) {
				eq.setStatus(EquipmentStatus.FREE);
				equipmentRepository.save(eq);
			}
			bookingRepository.delete(b);
		}
		for (Booking b : bookingRepository.findByDateToIsNull()) {
			Equipment eq = b.getEquipment();
			if (eq != null) {
				eq.setStatus(EquipmentStatus.FREE);
				equipmentRepository.save(eq);
			}
			bookingRepository.delete(b);
		}
	}
}
