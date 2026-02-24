package com.studio.crm_system.service;

import com.studio.crm_system.entity.Booking;
import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.entity.Rental;
import com.studio.crm_system.enums.EquipmentStatus;
import com.studio.crm_system.enums.RentalStatus;
import com.studio.crm_system.repository.BookingRepository;
import com.studio.crm_system.repository.EquipmentRepository;
import com.studio.crm_system.repository.RentalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

	@Autowired
	private BookingRepository bookingRepository;

	@Autowired
	private EquipmentRepository equipmentRepository;

	@Autowired
	private RentalRepository rentalRepository;

	public List<Booking> findAll() {
		return bookingRepository.findAllByOrderByCreatedAtDesc();
	}

	public Optional<Booking> findById(Long id) {
		return bookingRepository.findById(id);
	}

	/** Все брони по оборудованию для истории экземпляра (по дате окончания). */
	public List<Booking> findBookingsByEquipmentId(Long equipmentId) {
		return bookingRepository.findByEquipmentIdOrderByDateToDesc(equipmentId);
	}

	/** Конец текущей брони по оборудованию (минимальная dateTo среди активных броней) — для отображения «Забронирован до …». */
	public Optional<LocalDateTime> getBookingEndDateForEquipment(Long equipmentId) {
		List<Booking> list = bookingRepository.findBookingsContainingEquipmentAndDateToAfter(equipmentId, LocalDateTime.now());
		return list.stream().map(Booking::getDateTo).min(LocalDateTime::compareTo);
	}

	/** Максимальная dateTo среди активных броней по оборудованию — новая бронь «на потом» должна начинаться не раньше этой даты. */
	public Optional<LocalDateTime> getMaxBookingEndForEquipment(Long equipmentId) {
		List<Booking> list = bookingRepository.findBookingsContainingEquipmentAndDateToAfter(equipmentId, LocalDateTime.now());
		return list.stream().map(Booking::getDateTo).max(LocalDateTime::compareTo);
	}

	/** Последняя по дате окончания будущая бронь по оборудованию — для отображения «с … по …». */
	public Optional<Booking> getLastFutureBookingForEquipment(Long equipmentId) {
		List<Booking> list = bookingRepository.findBookingsContainingEquipmentAndDateToAfter(equipmentId, LocalDateTime.now());
		return list.stream().max(Comparator.comparing(Booking::getDateTo));
	}

	/** Текущая активная бронь по оборудованию (уже началась, ещё не истекла) — для отображения «с … по …». */
	public Optional<Booking> getActiveBookingForEquipment(Long equipmentId) {
		List<Booking> list = bookingRepository.findBookingsContainingEquipmentActiveAt(equipmentId, LocalDateTime.now());
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
	}

	/** Есть ли у оборудования бронь (текущая или будущая): dateTo > now. Нужно для блокировки удаления экземпляра на складе. */
	public boolean hasActiveOrFutureBooking(Long equipmentId) {
		return !bookingRepository.findBookingsContainingEquipmentAndDateToAfter(equipmentId, LocalDateTime.now()).isEmpty();
	}

	public List<Equipment> findFreeEquipment() {
		return equipmentRepository.findByStatusAndIsDeletedFalse(EquipmentStatus.FREE);
	}

	private static final int MAX_EQUIPMENT_PER_BOOKING = 50;

	@Transactional
	public String create(String phoneNumber, Long equipmentId, LocalDateTime dateFrom, LocalDateTime dateTo, String comment) {
		return createBatch(phoneNumber, List.of(equipmentId), dateFrom, dateTo, comment);
	}

	@Transactional
	public String createBatch(String phoneNumber, List<Long> equipmentIds, LocalDateTime dateFrom, LocalDateTime dateTo, String comment) {
		if (phoneNumber == null || phoneNumber.isBlank()) return "phone_required";
		if (equipmentIds == null || equipmentIds.isEmpty()) return "equipment_required";
		if (equipmentIds.size() > MAX_EQUIPMENT_PER_BOOKING) return "too_many_equipment";
		if (dateTo == null) return "date_to_required";
		if (!dateTo.isAfter(LocalDateTime.now())) return "date_to_in_past";
		if (dateFrom != null && !dateFrom.isBefore(dateTo)) return "date_to_before_from";

		LocalDateTime bookingStart = dateFrom != null ? dateFrom : LocalDateTime.now();

		List<Equipment> toReserve = new ArrayList<>();
		for (Long eid : equipmentIds) {
			Equipment equipment = equipmentRepository.findByIdAndIsDeletedFalse(eid).orElse(null);
			if (equipment == null) return "equipment_not_found";
			// Разрешаем бронировать свободное, в прокате и уже забронированное (бронь на бронь — цепочка)
			if (equipment.getStatus() != EquipmentStatus.FREE && equipment.getStatus() != EquipmentStatus.BUSY && equipment.getStatus() != EquipmentStatus.RESERVED)
				return "equipment_not_free";
			// Если оборудование в прокате — начало брони не раньше окончания проката
			if (equipment.getStatus() == EquipmentStatus.BUSY) {
				Optional<Rental> activeRental = rentalRepository.findFirstByEquipmentIdAndStatusInOrderByDateToDesc(
						equipment.getId(), List.of(RentalStatus.ACTIVE, RentalStatus.SOON_DEBTOR, RentalStatus.DEBTOR));
				if (activeRental.isPresent() && !bookingStart.isAfter(activeRental.get().getDateTo())) {
					return "booking_start_before_rental_end";
				}
			}
			// Новая бронь не должна пересекаться ни с одной существующей (можно ставить и до, и после)
			List<Booking> existing = bookingRepository.findBookingsContainingEquipmentAndDateToAfter(equipment.getId(), LocalDateTime.now());
			for (Booking b : existing) {
				LocalDateTime existingStart = b.getDateFrom() != null ? b.getDateFrom() : b.getCreatedAt();
				boolean overlap = bookingStart.isBefore(b.getDateTo()) && dateTo.isAfter(existingStart);
				if (overlap) {
					return "booking_overlap";
				}
			}
			toReserve.add(equipment);
		}

		Booking booking = new Booking();
		booking.setPhoneNumber(phoneNumber.trim());
		booking.setEquipmentList(toReserve);
		booking.setDateFrom(dateFrom);
		booking.setDateTo(dateTo);
		if (comment != null && !comment.isBlank()) booking.setComment(comment.trim());
		bookingRepository.save(booking);

		LocalDateTime now = LocalDateTime.now();
		for (Equipment equipment : toReserve) {
			// В резерв переводим только свободное и только если бронь уже началась (с момента начала брони)
			// Если dateFrom в будущем — оборудование остаётся FREE, кто-то может взять в прокат до начала брони
			if (equipment.getStatus() == EquipmentStatus.FREE && !bookingStart.isAfter(now)) {
				equipment.setStatus(EquipmentStatus.RESERVED);
				equipmentRepository.save(equipment);
			}
		}
		return null;
	}

	/**
	 * Вызывается при завершении/отмене проката: освобождённое оборудование переводится в FREE
	 * или в RESERVED, если на него есть активная бронь (dateTo в будущем).
	 */
	@Transactional
	public void releaseEquipment(Long equipmentId) {
		Equipment equipment = equipmentRepository.findByIdAndIsDeletedFalse(equipmentId).orElse(null);
		if (equipment == null) return;
		LocalDateTime now = LocalDateTime.now();
		// Учитываем только брони, которые уже начались (предмет занят с момента начала брони)
		boolean hasActiveBooking = !bookingRepository.findBookingsContainingEquipmentActiveAt(equipmentId, now).isEmpty();
		equipment.setStatus(hasActiveBooking ? EquipmentStatus.RESERVED : EquipmentStatus.FREE);
		equipmentRepository.save(equipment);
	}

	@Transactional
	public String delete(Long id) {
		Booking booking = bookingRepository.findById(id).orElse(null);
		if (booking == null) return "not_found";

		List<Long> equipmentIds = new ArrayList<>();
		for (Equipment eq : booking.getEquipmentList()) {
			equipmentIds.add(eq.getId());
		}
		bookingRepository.delete(booking);
		for (Long eqId : equipmentIds) {
			releaseEquipment(eqId);
		}
		return null;
	}

	/**
	 * Активирует брони с наступившей датой начала: переводит оборудование в RESERVED.
	 * Вызывается по расписанию — тогда оборудование «занято» только с момента начала брони.
	 */
	@Transactional
	public void activateBookings() {
		LocalDateTime now = LocalDateTime.now();
		for (Booking b : bookingRepository.findBookingsStartedAndNotEnded(now)) {
			for (Equipment eq : b.getEquipmentList()) {
				if (eq.getStatus() == EquipmentStatus.FREE) {
					eq.setStatus(EquipmentStatus.RESERVED);
					equipmentRepository.save(eq);
				}
			}
		}
	}

	@Transactional
	public void deleteExpiredBookings() {
		LocalDateTime now = LocalDateTime.now();
		for (Booking b : bookingRepository.findByDateToLessThanEqual(now)) {
			List<Long> equipmentIds = new ArrayList<>();
			for (Equipment eq : b.getEquipmentList()) {
				equipmentIds.add(eq.getId());
			}
			bookingRepository.delete(b);
			for (Long eqId : equipmentIds) {
				releaseEquipment(eqId);
			}
		}
		for (Booking b : bookingRepository.findByDateToIsNull()) {
			List<Long> equipmentIds = new ArrayList<>();
			for (Equipment eq : b.getEquipmentList()) {
				equipmentIds.add(eq.getId());
			}
			bookingRepository.delete(b);
			for (Long eqId : equipmentIds) {
				releaseEquipment(eqId);
			}
		}
	}
}
