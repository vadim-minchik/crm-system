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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RentalService {

	@Autowired private RentalRepository rentalRepository;
	@Autowired private ClientRepository clientRepository;
	@Autowired private EquipmentRepository equipmentRepository;

	/** Список всех прокатов (новые сверху) */
	public List<Rental> findAll() {
		return rentalRepository.findAllByOrderByDateFromDesc();
	}

	/** Активные (в прокате) */
	public List<Rental> findActive() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.ACTIVE);
	}

	/** История — завершённые */
	public List<Rental> findCompleted() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.COMPLETED);
	}

	/** Должники — просрочили возврат */
	public List<Rental> findDebtors() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.DEBTOR);
	}

	/** Приёмка — скоро срок возврата */
	public List<Rental> findSoonDebtors() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.SOON_DEBTOR);
	}

	/** Отменённые прокаты */
	public List<Rental> findCancelled() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.CANCELLED);
	}

	public Optional<Rental> findById(Long id) {
		return rentalRepository.findById(id);
	}

	public Optional<Client> findClientById(Long id) {
		return clientRepository.findByIdAndIsDeletedFalse(id);
	}

	public Optional<Equipment> findEquipmentById(Long id) {
		return equipmentRepository.findByIdAndIsDeletedFalse(id);
	}

	/** Свободное оборудование для выбора в форме */
	public List<Equipment> findFreeEquipment() {
		return equipmentRepository.findByStatusAndIsDeletedFalse(EquipmentStatus.FREE);
	}

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	/**
	 * Все оборудование (не удалённое) с подписью статуса для выпадающего списка:
	 * «Свободен», «В прокате до ДД.ММ.ГГГГ», «Забронирован».
	 */
	public List<EquipmentSelectOption> getEquipmentOptionsForSelect() {
		List<Equipment> all = equipmentRepository.findByIsDeletedFalse();
		return all.stream().map(e -> {
			String statusLabel;
			boolean free = (e.getStatus() == EquipmentStatus.FREE);
			if (e.getStatus() == EquipmentStatus.FREE) {
				statusLabel = "Свободен";
			} else if (e.getStatus() == EquipmentStatus.BUSY) {
				Optional<Rental> active = rentalRepository.findFirstByEquipment_IdAndStatusInOrderByDateToDesc(e.getId(), java.util.List.of(RentalStatus.ACTIVE, RentalStatus.SOON_DEBTOR, RentalStatus.DEBTOR));
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

	/**
	 * Рассчитать итог по прокату по дате-времени:
	 * — меньше суток: по часам (pricePerHour);
	 * — сутки и больше: полные дни по pricePerDay + остаток часов по pricePerHour.
	 */
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

	/**
	 * Несколько прокатов: один клиент, одни даты, по одному прокату на каждый инструмент (1–50).
	 */
	@Transactional
	public String createRentals(Long clientId, List<Long> equipmentIds, LocalDateTime dateFrom, LocalDateTime dateTo) {
		if (clientId == null) return "client_required";
		if (equipmentIds == null || equipmentIds.isEmpty()) return "equipment_required";
		if (equipmentIds.size() > MAX_EQUIPMENT_PER_RENTAL) return "too_many_equipment";
		if (dateFrom == null) return "date_from_required";
		if (dateTo == null) return "date_to_required";
		if (!dateTo.isAfter(dateFrom)) return "date_to_before_from";
		if (dateFrom.isBefore(LocalDateTime.now())) return "date_from_in_past";

		Client client = clientRepository.findByIdAndIsDeletedFalse(clientId).orElse(null);
		if (client == null) return "client_not_found";

		for (Long eid : equipmentIds) {
			Equipment equipment = equipmentRepository.findByIdAndIsDeletedFalse(eid).orElse(null);
			if (equipment == null) return "equipment_not_found";
			if (equipment.getStatus() != EquipmentStatus.FREE) return "equipment_not_free";

			BigDecimal total = calculateTotal(dateFrom, dateTo, equipment).setScale(2, RoundingMode.HALF_UP);
			Rental rental = new Rental();
			rental.setClient(client);
			rental.setEquipment(equipment);
			rental.setDateFrom(dateFrom);
			rental.setDateTo(dateTo);
			rental.setTotalAmount(total);
			rental.setStatus(RentalStatus.ACTIVE);
			rentalRepository.save(rental);
			equipment.setStatus(EquipmentStatus.BUSY);
			equipmentRepository.save(equipment);
		}
		return null;
	}

	/**
	 * Обновить прокат: период и итог (редактирование с карточки проката).
	 */
	@Transactional
	public String updateRental(Long id, LocalDateTime dateFrom, LocalDateTime dateTo, BigDecimal totalAmount) {
		Rental rental = rentalRepository.findById(id).orElse(null);
		if (rental == null) return "not_found";
		if (dateFrom == null) return "date_from_required";
		if (dateTo == null) return "date_to_required";
		if (!dateTo.isAfter(dateFrom)) return "date_to_before_from";

		rental.setDateFrom(dateFrom);
		rental.setDateTo(dateTo);
		BigDecimal total = totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0
				? totalAmount.setScale(2, RoundingMode.HALF_UP)
				: calculateTotal(dateFrom, dateTo, rental.getEquipment()).setScale(2, RoundingMode.HALF_UP);
		rental.setTotalAmount(total);
		rentalRepository.save(rental);
		return null;
	}

	/**
	 * Завершить прокат: статус COMPLETED, оборудованию вернуть FREE.
	 * Разрешено для ACTIVE, SOON_DEBTOR, DEBTOR (вручную завершают только сотрудники).
	 * Используется блокировка строки (PESSIMISTIC_WRITE): если два работника нажали «Завершить» на одном прокате,
	 * второй подождёт, получит уже завершённый прокат и увидит «Прокат уже завершён или отменён».
	 */
	@Transactional
	public String completeRental(Long rentalId) {
		Rental rental = rentalRepository.findByIdForUpdate(rentalId).orElse(null);
		if (rental == null) return "not_found";
		RentalStatus s = rental.getStatus();
		if (s != RentalStatus.ACTIVE && s != RentalStatus.SOON_DEBTOR && s != RentalStatus.DEBTOR)
			return "not_active";

		rental.setStatus(RentalStatus.COMPLETED);
		rentalRepository.save(rental);

		Equipment eq = rental.getEquipment();
		if (eq != null) {
			eq.setStatus(EquipmentStatus.FREE);
			equipmentRepository.save(eq);
		}
		return null;
	}

	/**
	 * Отменить прокат: статус CANCELLED, оборудованию вернуть FREE.
	 * Разрешено только для ACTIVE (из должников и приёмки — только «Завершить» в историю).
	 */
	@Transactional
	public String cancelRental(Long rentalId) {
		Rental rental = rentalRepository.findByIdForUpdate(rentalId).orElse(null);
		if (rental == null) return "not_found";
		if (rental.getStatus() != RentalStatus.ACTIVE) return "not_active";

		rental.setStatus(RentalStatus.CANCELLED);
		rentalRepository.save(rental);

		Equipment eq = rental.getEquipment();
		if (eq != null) {
			eq.setStatus(EquipmentStatus.FREE);
			equipmentRepository.save(eq);
		}
		return null;
	}

	/**
	 * Фоновая перераспределение: прокаты с ACTIVE/SOON_DEBTOR/DEBTOR проверяются по времени.
	 * — Если сейчас >= dateTo → DEBTOR (должник).
	 * — Иначе если в зоне «приёмка»: по часам аренды 20 мин до конца, по дням 1 час до конца, по неделе 1 день до конца → SOON_DEBTOR.
	 * — Иначе → ACTIVE.
	 */
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
