package com.studio.crm_system.service;

import com.studio.crm_system.dto.EquipmentSelectOption;
import com.studio.crm_system.entity.Client;
import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.entity.Rental;
import com.studio.crm_system.enums.EquipmentStatus;
import com.studio.crm_system.enums.RentalStatus;
import com.studio.crm_system.repository.ClientRepository;
import com.studio.crm_system.repository.EquipmentRepository;
import com.studio.crm_system.repository.RentalRepository;
import com.studio.crm_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RentalService {

	@Autowired private RentalRepository rentalRepository;
	@Autowired private ClientRepository clientRepository;
	@Autowired private EquipmentRepository equipmentRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private BookingService bookingService;

	public List<Rental> findAll() {
		return rentalRepository.findAllByOrderByDateFromDesc();
	}

	public List<Rental> findActive() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.ACTIVE);
	}

	public List<Rental> findCompleted() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.COMPLETED);
	}

	public List<Rental> findDebtors() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.DEBTOR);
	}

	public List<Rental> findSoonDebtors() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.SOON_DEBTOR);
	}

	public List<Rental> findCancelled() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.CANCELLED);
	}

	public Optional<Rental> findById(Long id) {
		return rentalRepository.findByIdWithEquipment(id);
	}

	/** Прокат с клиентом и связями для формирования документа (подстановки в шаблоне). */
	public Optional<Rental> findByIdForDocument(Long id) {
		return rentalRepository.findByIdForDocument(id);
	}

	/** Все прокаты по оборудованию для истории экземпляра. */
	public List<Rental> findRentalsByEquipmentId(Long equipmentId) {
		return rentalRepository.findByEquipmentIdOrderByDateFromDesc(equipmentId);
	}

	public Optional<Client> findClientById(Long id) {
		return clientRepository.findByIdAndIsDeletedFalse(id);
	}

	public Optional<Equipment> findEquipmentById(Long id) {
		return equipmentRepository.findByIdAndIsDeletedFalse(id);
	}

	public List<Equipment> findFreeEquipment() {
		return equipmentRepository.findByStatusAndIsDeletedFalse(EquipmentStatus.FREE);
	}

	private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	public List<EquipmentSelectOption> getEquipmentOptionsForSelect() {
		List<Equipment> all = equipmentRepository.findByIsDeletedFalse();
		return all.stream().map(e -> toEquipmentSelectOption(e)).collect(Collectors.toList());
	}

	/** Оборудование для выбора в форме брони: свободное, в прокате и уже забронированное (цепочка броней). */
	public List<EquipmentSelectOption> getEquipmentOptionsForBooking() {
		List<Equipment> list = equipmentRepository.findByStatusInAndIsDeletedFalse(
				java.util.List.of(EquipmentStatus.FREE, EquipmentStatus.BUSY, EquipmentStatus.RESERVED));
		return list.stream().map(e -> toEquipmentSelectOption(e)).collect(Collectors.toList());
	}

	private EquipmentSelectOption toEquipmentSelectOption(Equipment e) {
		String statusLabel;
		boolean free = (e.getStatus() == EquipmentStatus.FREE);
		if (e.getStatus() == EquipmentStatus.FREE) {
			// Свободен сейчас, но если впереди есть брони — показываем с и по (последняя в цепочке)
			statusLabel = bookingService.getLastFutureBookingForEquipment(e.getId())
					.map(b -> {
						LocalDateTime from = b.getDateFrom() != null ? b.getDateFrom() : b.getCreatedAt();
						return "Свободен, далее забронировано с " + from.format(DATETIME_FMT) + " по " + b.getDateTo().format(DATETIME_FMT);
					})
					.orElse("Свободен");
		} else if (e.getStatus() == EquipmentStatus.BUSY) {
			Optional<Rental> active = rentalRepository.findFirstByEquipmentIdAndStatusInOrderByDateToDesc(e.getId(), java.util.List.of(RentalStatus.ACTIVE, RentalStatus.SOON_DEBTOR, RentalStatus.DEBTOR));
			statusLabel = active.map(r -> "В прокате с " + r.getDateFrom().format(DATETIME_FMT) + " по " + r.getDateTo().format(DATETIME_FMT)).orElse("В прокате");
		} else {
			// RESERVED: показываем с и по текущей брони
			statusLabel = bookingService.getActiveBookingForEquipment(e.getId())
					.map(b -> {
						LocalDateTime from = b.getDateFrom() != null ? b.getDateFrom() : b.getCreatedAt();
						return "Забронирован с " + from.format(DATETIME_FMT) + " по " + b.getDateTo().format(DATETIME_FMT);
					})
					.orElse("Забронирован");
		}
		return new EquipmentSelectOption(
				e.getId(),
				e.getTitle(),
				e.getSerialNumber(),
				statusLabel,
				e.getPriceFirstDay(),
				e.getPriceSecondDay(),
				e.getPriceSubsequentDays(),
				free
		);
	}

	public List<Client> findAllClients() {
		return clientRepository.findByIsDeletedFalse();
	}

	/** Считает сумму по полным дням: 1-й день X, 2-й 0.8X, далее 0.6X за день (значения из equipment). */
	public BigDecimal calculateTotal(LocalDateTime dateFrom, LocalDateTime dateTo, Equipment equipment) {
		if (dateFrom == null || dateTo == null || equipment == null) return BigDecimal.ZERO;
		if (!dateTo.isAfter(dateFrom)) return BigDecimal.ZERO;
		long fullDays = ChronoUnit.DAYS.between(dateFrom.toLocalDate(), dateTo.toLocalDate());
		if (fullDays <= 0) return BigDecimal.ZERO;
		BigDecimal d1 = equipment.getPriceFirstDay();
		BigDecimal d2 = equipment.getPriceSecondDay();
		BigDecimal dSub = equipment.getPriceSubsequentDays();
		if (fullDays == 1) return d1.setScale(2, RoundingMode.HALF_UP);
		if (fullDays == 2) return d1.add(d2).setScale(2, RoundingMode.HALF_UP);
		return d1.add(d2).add(dSub.multiply(BigDecimal.valueOf(fullDays - 2))).setScale(2, RoundingMode.HALF_UP);
	}

	private static final int MAX_EQUIPMENT_PER_RENTAL = 50;

	@Transactional
	public String createRentals(Long clientId, List<Long> equipmentIds, LocalDateTime dateFrom, LocalDateTime dateTo, BigDecimal manualTotal,
			BigDecimal additionalServicesAmount, String additionalServicesDescription, BigDecimal deliveryAmount, String deliveryAddress,
			Long createdByStaffId, Long handedOverByStaffId) {
		if (clientId == null) return "client_required";
		if (equipmentIds == null || equipmentIds.isEmpty()) return "equipment_required";
		if (equipmentIds.size() > MAX_EQUIPMENT_PER_RENTAL) return "too_many_equipment";
		if (dateFrom == null) return "date_from_required";
		if (dateTo == null) return "date_to_required";
		if (!dateTo.isAfter(dateFrom)) return "date_to_before_from";
		if (dateFrom.isBefore(LocalDateTime.now())) return "date_from_in_past";

		Client client = clientRepository.findByIdAndIsDeletedFalse(clientId).orElse(null);
		if (client == null) return "client_not_found";

		List<Equipment> toRent = new ArrayList<>();
		for (Long eid : equipmentIds) {
			Equipment equipment = equipmentRepository.findByIdAndIsDeletedFalse(eid).orElse(null);
			if (equipment == null) return "equipment_not_found";
			if (equipment.getStatus() != EquipmentStatus.FREE) return "equipment_not_free";
			toRent.add(equipment);
		}

		BigDecimal baseSum;
		if (manualTotal != null && manualTotal.compareTo(BigDecimal.ZERO) > 0) {
			baseSum = manualTotal.setScale(2, RoundingMode.HALF_UP);
		} else {
			baseSum = BigDecimal.ZERO;
			for (Equipment e : toRent) {
				baseSum = baseSum.add(calculateTotal(dateFrom, dateTo, e));
			}
			baseSum = baseSum.setScale(2, RoundingMode.HALF_UP);
		}

		BigDecimal addServ = (additionalServicesAmount != null && additionalServicesAmount.compareTo(BigDecimal.ZERO) > 0)
				? additionalServicesAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
		BigDecimal delAmt = (deliveryAmount != null && deliveryAmount.compareTo(BigDecimal.ZERO) > 0)
				? deliveryAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
		BigDecimal totalSum = baseSum.add(addServ).add(delAmt).setScale(2, RoundingMode.HALF_UP);

		Rental rental = new Rental();
		rental.setClient(client);
		rental.setEquipmentList(toRent);
		rental.setDateFrom(dateFrom);
		rental.setDateTo(dateTo);
		rental.setTotalAmount(totalSum);
		rental.setAdditionalServicesAmount(addServ.compareTo(BigDecimal.ZERO) > 0 ? addServ : null);
		rental.setAdditionalServicesDescription(additionalServicesDescription != null && !additionalServicesDescription.isBlank() ? additionalServicesDescription.trim() : null);
		rental.setDeliveryAmount(delAmt.compareTo(BigDecimal.ZERO) > 0 ? delAmt : null);
		rental.setDeliveryAddress(deliveryAddress != null && !deliveryAddress.isBlank() ? deliveryAddress.trim() : null);
		rental.setStatus(RentalStatus.ACTIVE);
		if (createdByStaffId != null) userRepository.findById(createdByStaffId).ifPresent(rental::setCreatedByStaff);
		if (handedOverByStaffId != null) userRepository.findById(handedOverByStaffId).ifPresent(rental::setHandedOverByStaff);
		rentalRepository.save(rental);

		for (Equipment equipment : toRent) {
			equipment.setStatus(EquipmentStatus.BUSY);
			equipmentRepository.save(equipment);
		}
		return null;
	}

	@Transactional
	public String updateRental(Long id, LocalDateTime dateFrom, LocalDateTime dateTo, BigDecimal totalAmount,
			BigDecimal additionalServicesAmount, String additionalServicesDescription, BigDecimal deliveryAmount, String deliveryAddress,
			Long createdByStaffId, Long handedOverByStaffId) {
		Rental rental = rentalRepository.findByIdWithEquipment(id).orElse(null);
		if (rental == null) return "not_found";
		if (dateFrom == null) return "date_from_required";
		if (dateTo == null) return "date_to_required";
		if (!dateTo.isAfter(dateFrom)) return "date_to_before_from";

		rental.setDateFrom(dateFrom);
		rental.setDateTo(dateTo);
		List<Equipment> list = rental.getEquipmentList();
		BigDecimal total = totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0
				? totalAmount.setScale(2, RoundingMode.HALF_UP)
				: (list.isEmpty()
						? BigDecimal.ZERO
						: list.stream()
								.map(e -> calculateTotal(dateFrom, dateTo, e))
								.reduce(BigDecimal.ZERO, BigDecimal::add)).setScale(2, RoundingMode.HALF_UP);
		rental.setTotalAmount(total);
		rental.setAdditionalServicesAmount(additionalServicesAmount != null && additionalServicesAmount.compareTo(BigDecimal.ZERO) > 0 ? additionalServicesAmount.setScale(2, RoundingMode.HALF_UP) : null);
		rental.setAdditionalServicesDescription(additionalServicesDescription != null && !additionalServicesDescription.isBlank() ? additionalServicesDescription.trim() : null);
		rental.setDeliveryAmount(deliveryAmount != null && deliveryAmount.compareTo(BigDecimal.ZERO) > 0 ? deliveryAmount.setScale(2, RoundingMode.HALF_UP) : null);
		rental.setDeliveryAddress(deliveryAddress != null && !deliveryAddress.isBlank() ? deliveryAddress.trim() : null);
		if (createdByStaffId != null) userRepository.findById(createdByStaffId).ifPresent(rental::setCreatedByStaff);
		else rental.setCreatedByStaff(null);
		if (handedOverByStaffId != null) userRepository.findById(handedOverByStaffId).ifPresent(rental::setHandedOverByStaff);
		else rental.setHandedOverByStaff(null);
		rentalRepository.save(rental);
		return null;
	}

	@Transactional
	public String completeRental(Long rentalId) {
		Rental rental = rentalRepository.findByIdForUpdate(rentalId).orElse(null);
		if (rental == null) return "not_found";
		RentalStatus s = rental.getStatus();
		if (s != RentalStatus.ACTIVE && s != RentalStatus.SOON_DEBTOR && s != RentalStatus.DEBTOR)
			return "not_active";

		rental.setStatus(RentalStatus.COMPLETED);
		rentalRepository.save(rental);

		for (Equipment eq : rental.getEquipmentList()) {
			bookingService.releaseEquipment(eq.getId());
		}
		return null;
	}

	@Transactional
	public String cancelRental(Long rentalId) {
		Rental rental = rentalRepository.findByIdForUpdate(rentalId).orElse(null);
		if (rental == null) return "not_found";
		if (rental.getStatus() != RentalStatus.ACTIVE) return "not_active";

		rental.setStatus(RentalStatus.CANCELLED);
		rentalRepository.save(rental);

		for (Equipment eq : rental.getEquipmentList()) {
			bookingService.releaseEquipment(eq.getId());
		}
		return null;
	}

	@Transactional
	public void updateRentalStatuses() {
		LocalDateTime now = LocalDateTime.now();
		List<Rental> toCheck = rentalRepository.findByStatusInOrderByDateFromDesc(
				java.util.List.of(RentalStatus.ACTIVE, RentalStatus.SOON_DEBTOR, RentalStatus.DEBTOR));
		for (Rental r : toCheck) {
			LocalDateTime to = r.getDateTo();
			if (!now.isBefore(to)) {
				if (r.getStatus() != RentalStatus.DEBTOR) {
					r.setStatus(RentalStatus.DEBTOR);
					rentalRepository.save(r);
				}
				continue;
			}
			double hours = ChronoUnit.MINUTES.between(r.getDateFrom(), to) / 60.0;
			LocalDateTime threshold;
			if (hours <= 24) {
				threshold = to.minusMinutes(20);
			} else if (hours <= 24 * 7) {
				threshold = to.minusHours(1);
			} else {
				threshold = to.minusDays(1);
			}
			RentalStatus newStatus = !now.isBefore(threshold) ? RentalStatus.SOON_DEBTOR : RentalStatus.ACTIVE;
			if (r.getStatus() != newStatus) {
				r.setStatus(newStatus);
				rentalRepository.save(r);
			}
		}
	}
}
