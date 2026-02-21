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

	public Optional<Client> findClientById(Long id) {
		return clientRepository.findByIdAndIsDeletedFalse(id);
	}

	public Optional<Equipment> findEquipmentById(Long id) {
		return equipmentRepository.findByIdAndIsDeletedFalse(id);
	}

	public List<Equipment> findFreeEquipment() {
		return equipmentRepository.findByStatusAndIsDeletedFalse(EquipmentStatus.FREE);
	}

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	public List<EquipmentSelectOption> getEquipmentOptionsForSelect() {
		List<Equipment> all = equipmentRepository.findByIsDeletedFalse();
		return all.stream().map(e -> {
			String statusLabel;
			boolean free = (e.getStatus() == EquipmentStatus.FREE);
			if (e.getStatus() == EquipmentStatus.FREE) {
				statusLabel = "Свободен";
			} else if (e.getStatus() == EquipmentStatus.BUSY) {
				Optional<Rental> active = rentalRepository.findFirstByEquipmentIdAndStatusInOrderByDateToDesc(e.getId(), java.util.List.of(RentalStatus.ACTIVE, RentalStatus.SOON_DEBTOR, RentalStatus.DEBTOR));
				statusLabel = active.map(r -> "В прокате до " + r.getDateTo().toLocalDate().format(DATE_FMT)).orElse("В прокате");
			} else {
				statusLabel = "Забронирован";
			}
			return new EquipmentSelectOption(
					e.getId(),
					e.getTitle(),
					e.getSerialNumber(),
					statusLabel,
					e.getPricePerDay(),
					e.getPricePerWeek(),
					e.getPricePerHour(),
					free
			);
		}).collect(Collectors.toList());
	}

	public List<Client> findAllClients() {
		return clientRepository.findByIsDeletedFalse();
	}

	public BigDecimal calculateTotal(LocalDateTime dateFrom, LocalDateTime dateTo, Equipment equipment) {
		if (dateFrom == null || dateTo == null || equipment == null) return BigDecimal.ZERO;
		if (!dateTo.isAfter(dateFrom)) return BigDecimal.ZERO;
		double hours = ChronoUnit.MINUTES.between(dateFrom, dateTo) / 60.0;
		if (hours < 24) {
			return equipment.getPricePerHour().multiply(BigDecimal.valueOf(hours)).setScale(2, RoundingMode.HALF_UP);
		}
		int fullDays = (int) (hours / 24);
		double remainderHours = hours - fullDays * 24;
		BigDecimal dayPart = equipment.getPricePerDay().multiply(BigDecimal.valueOf(fullDays));
		BigDecimal hourPart = equipment.getPricePerHour().multiply(BigDecimal.valueOf(remainderHours));
		return dayPart.add(hourPart).setScale(2, RoundingMode.HALF_UP);
	}

	private static final int MAX_EQUIPMENT_PER_RENTAL = 50;

	@Transactional
	public String createRentals(Long clientId, List<Long> equipmentIds, LocalDateTime dateFrom, LocalDateTime dateTo, BigDecimal manualTotal) {
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

		BigDecimal totalSum;
		if (manualTotal != null && manualTotal.compareTo(BigDecimal.ZERO) > 0) {
			totalSum = manualTotal.setScale(2, RoundingMode.HALF_UP);
		} else {
			totalSum = BigDecimal.ZERO;
			for (Equipment e : toRent) {
				totalSum = totalSum.add(calculateTotal(dateFrom, dateTo, e));
			}
			totalSum = totalSum.setScale(2, RoundingMode.HALF_UP);
		}

		Rental rental = new Rental();
		rental.setClient(client);
		rental.setEquipmentList(toRent);
		rental.setDateFrom(dateFrom);
		rental.setDateTo(dateTo);
		rental.setTotalAmount(totalSum);
		rental.setStatus(RentalStatus.ACTIVE);
		rentalRepository.save(rental);

		for (Equipment equipment : toRent) {
			equipment.setStatus(EquipmentStatus.BUSY);
			equipmentRepository.save(equipment);
		}
		return null;
	}

	@Transactional
	public String updateRental(Long id, LocalDateTime dateFrom, LocalDateTime dateTo, BigDecimal totalAmount) {
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
			eq.setStatus(EquipmentStatus.FREE);
			equipmentRepository.save(eq);
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
			eq.setStatus(EquipmentStatus.FREE);
			equipmentRepository.save(eq);
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
