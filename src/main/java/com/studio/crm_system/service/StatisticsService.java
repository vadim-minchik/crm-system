package com.studio.crm_system.service;

import com.studio.crm_system.entity.Booking;
import com.studio.crm_system.entity.Client;
import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.entity.Expense;
import com.studio.crm_system.entity.Point;
import com.studio.crm_system.entity.Rental;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.enums.EquipmentStatus;
import com.studio.crm_system.enums.RentalStatus;
import com.studio.crm_system.repository.BookingRepository;
import com.studio.crm_system.repository.ClientRepository;
import com.studio.crm_system.repository.EquipmentRepository;
import com.studio.crm_system.repository.ExpenseRepository;
import com.studio.crm_system.repository.PointRepository;
import com.studio.crm_system.repository.RentalRepository;
import com.studio.crm_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.studio.crm_system.dto.RevenueByMonthDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Сводные данные для раздела «Статистика».
 */
@Service
public class StatisticsService {

	@Autowired private ClientRepository clientRepository;
	@Autowired private EquipmentRepository equipmentRepository;
	@Autowired private RentalRepository rentalRepository;
	@Autowired private BookingRepository bookingRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private PointRepository pointRepository;
	@Autowired private ExpenseRepository expenseRepository;

	/** Количество клиентов (без удалённых). */
	public long getClientsCount() {
		return clientRepository.findByIsDeletedFalse().size();
	}

	/** Всего единиц оборудования (без удалённых). */
	public long getEquipmentTotalCount() {
		return equipmentRepository.findByIsDeletedFalse().size();
	}

	/** Свободное оборудование. */
	public long getEquipmentFreeCount() {
		return equipmentRepository.findByStatusAndIsDeletedFalse(EquipmentStatus.FREE).size();
	}

	/** Занятое оборудование (в прокате). */
	public long getEquipmentBusyCount() {
		return equipmentRepository.findByStatusAndIsDeletedFalse(EquipmentStatus.BUSY).size();
	}

	/** Забронированное оборудование. */
	public long getEquipmentReservedCount() {
		return equipmentRepository.findByStatusAndIsDeletedFalse(EquipmentStatus.RESERVED).size();
	}

	/** Количество активных прокатов. */
	public long getActiveRentalsCount() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.ACTIVE).size();
	}

	/** Оформлены с доставкой, ждут выдачи клиенту. */
	public long getAwaitingDeliveryRentalsCount() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.AWAITING_DELIVERY).size();
	}

	/** Количество завершённых прокатов. */
	public long getCompletedRentalsCount() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.COMPLETED).size();
	}

	/** Количество должников (DEBTOR). */
	public long getDebtorsCount() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.DEBTOR).size();
	}

	/** Количество броней (всего в системе, для ориентира). */
	public long getBookingsCount() {
		return bookingRepository.findAllByOrderByCreatedAtDesc().size();
	}

	/** Количество сотрудников (без удалённых). */
	public long getStaffCount() {
		return userRepository.findByIsDeletedFalse().size();
	}

	/** Количество точек выдачи (без удалённых). */
	public long getPointsCount() {
		return pointRepository.findByIsDeletedFalseOrderByNameAsc().size();
	}

	/** Сумма выручки по завершённым прокатам. */
	public BigDecimal getTotalRevenue() {
		List<Rental> completed = rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.COMPLETED);
		return completed.stream()
				.map(Rental::getTotalAmount)
				.filter(a -> a != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// ——— Списки для модальных панелей (кликабельная статистика) ———

	public List<Client> getClientsList() {
		return clientRepository.findByIsDeletedFalse();
	}

	public List<Equipment> getFreeEquipmentList() {
		return equipmentRepository.findByStatusAndIsDeletedFalse(EquipmentStatus.FREE);
	}

	public List<Equipment> getBusyEquipmentList() {
		return equipmentRepository.findByStatusAndIsDeletedFalse(EquipmentStatus.BUSY);
	}

	public List<Equipment> getReservedEquipmentList() {
		return equipmentRepository.findByStatusAndIsDeletedFalse(EquipmentStatus.RESERVED);
	}

	public List<Equipment> getAllEquipmentList() {
		return equipmentRepository.findByIsDeletedFalse();
	}

	public List<Rental> getActiveRentalsList() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.ACTIVE);
	}

	public List<Rental> getCompletedRentalsList() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.COMPLETED);
	}

	public List<Rental> getDebtorsList() {
		return rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.DEBTOR);
	}

	/** Все прокаты (для полной таблицы со сортировкой). */
	public List<Rental> getAllRentalsList() {
		return rentalRepository.findAllByOrderByDateFromDesc();
	}

	public List<Booking> getBookingsList() {
		return bookingRepository.findAllByOrderByCreatedAtDesc();
	}

	public List<User> getStaffList() {
		return userRepository.findByIsDeletedFalse();
	}

	public List<Point> getPointsList() {
		return pointRepository.findByIsDeletedFalseOrderByNameAsc();
	}

	// ——— Данные для графиков ———

	private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM.yyyy");
	private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd.MM.yy");
	private static final DateTimeFormatter WEEK_FMT = DateTimeFormatter.ofPattern("dd.MM");
	private static final int REVENUE_MONTHS = 12;
	private static final int MAX_DAYS_FOR_DAILY = 62;

	/** Выручка за произвольный период (по дням или по неделям). Прокаты учитываются по дате окончания (dateTo). */
	public List<RevenueByMonthDto> getRevenueByDateRange(LocalDate from, LocalDate to) {
		if (from == null || to == null || from.isAfter(to)) return getRevenueByMonthLast12();
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		if (days > 366) return getRevenueByMonthLast12();

		List<Rental> completed = rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.COMPLETED);
		LocalDateTime fromTime = from.atStartOfDay();
		LocalDateTime toTime = to.atTime(23, 59, 59);

		boolean byWeek = days > MAX_DAYS_FOR_DAILY;
		WeekFields wf = WeekFields.of(Locale.getDefault());
		Map<LocalDate, BigDecimal> byPeriodRaw = new TreeMap<>();

		if (byWeek) {
			LocalDate weekStart = from.with(wf.dayOfWeek(), 1);
			for (LocalDate w = weekStart; !w.isAfter(to); w = w.plusWeeks(1)) {
				if (!w.plusDays(6).isBefore(from)) byPeriodRaw.put(w, BigDecimal.ZERO);
			}
		} else {
			for (long i = 0; i < days; i++) {
				byPeriodRaw.put(from.plusDays(i), BigDecimal.ZERO);
			}
		}

		for (Rental r : completed) {
			if (r.getDateTo() == null) continue;
			LocalDateTime dt = r.getDateTo();
			if (dt.isBefore(fromTime) || dt.isAfter(toTime)) continue;
			LocalDate d = dt.toLocalDate();
			BigDecimal amt = r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO;

			if (byWeek) {
				LocalDate key = d.with(wf.dayOfWeek(), 1);
				if (key.isBefore(from)) key = key.plusWeeks(1);
				byPeriodRaw.merge(key, amt, BigDecimal::add);
			} else {
				byPeriodRaw.merge(d, amt, BigDecimal::add);
			}
		}

		Map<String, BigDecimal> byPeriod = new LinkedHashMap<>();
		for (Map.Entry<LocalDate, BigDecimal> e : byPeriodRaw.entrySet()) {
			String label = byWeek
				? (e.getKey().format(WEEK_FMT) + " – " + e.getKey().plusDays(6).format(WEEK_FMT))
				: e.getKey().format(DAY_FMT);
			byPeriod.put(label, e.getValue());
		}

		List<RevenueByMonthDto> result = new ArrayList<>();
		for (Map.Entry<String, BigDecimal> e : byPeriod.entrySet()) {
			result.add(new RevenueByMonthDto(e.getKey(), e.getValue()));
		}
		return result;
	}

	/** Выручка по месяцам (последние 12 месяцев) для графика. */
	public List<RevenueByMonthDto> getRevenueByMonthLast12() {
		List<Rental> completed = rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.COMPLETED);
		Map<YearMonth, BigDecimal> byMonth = new LinkedHashMap<>();
		YearMonth now = YearMonth.now();
		for (int i = REVENUE_MONTHS - 1; i >= 0; i--) {
			byMonth.put(now.minusMonths(i), BigDecimal.ZERO);
		}
		for (Rental r : completed) {
			if (r.getDateTo() == null) continue;
			YearMonth ym = YearMonth.from(r.getDateTo());
			if (!byMonth.containsKey(ym)) continue;
			BigDecimal amt = r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO;
			byMonth.put(ym, byMonth.get(ym).add(amt));
		}
		List<RevenueByMonthDto> result = new ArrayList<>();
		for (Map.Entry<YearMonth, BigDecimal> e : byMonth.entrySet()) {
			result.add(new RevenueByMonthDto(e.getKey().format(MONTH_FMT), e.getValue()));
		}
		return result;
	}

	/** Суммы расходов по тем же 12 месяцам, что и getRevenueByMonthLast12 (тот же порядок). */
	public List<BigDecimal> getExpensesByMonthLast12() {
		List<Expense> all = expenseRepository.findByIsDeletedFalseOrderByExpenseDateDesc();
		Map<YearMonth, BigDecimal> byMonth = new LinkedHashMap<>();
		YearMonth now = YearMonth.now();
		for (int i = REVENUE_MONTHS - 1; i >= 0; i--) {
			byMonth.put(now.minusMonths(i), BigDecimal.ZERO);
		}
		for (Expense e : all) {
			if (e.getExpenseDate() == null) continue;
			YearMonth ym = YearMonth.from(e.getExpenseDate());
			if (!byMonth.containsKey(ym)) continue;
			BigDecimal amt = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
			byMonth.put(ym, byMonth.get(ym).add(amt));
		}
		return new ArrayList<>(byMonth.values());
	}

	/** Расходы за тот же период и с теми же подписями, что и getRevenueByDateRange. */
	public List<RevenueByMonthDto> getExpensesByDateRange(LocalDate from, LocalDate to) {
		if (from == null || to == null || from.isAfter(to)) return List.of();
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		if (days > 366) return List.of();

		List<Expense> all = expenseRepository.findByIsDeletedFalseOrderByExpenseDateDesc();
		boolean byWeek = days > MAX_DAYS_FOR_DAILY;
		WeekFields wf = WeekFields.of(Locale.getDefault());
		Map<LocalDate, BigDecimal> byPeriodRaw = new TreeMap<>();

		if (byWeek) {
			LocalDate weekStart = from.with(wf.dayOfWeek(), 1);
			for (LocalDate w = weekStart; !w.isAfter(to); w = w.plusWeeks(1)) {
				if (!w.plusDays(6).isBefore(from)) byPeriodRaw.put(w, BigDecimal.ZERO);
			}
		} else {
			for (long i = 0; i < days; i++) {
				byPeriodRaw.put(from.plusDays(i), BigDecimal.ZERO);
			}
		}

		for (Expense e : all) {
			if (e.getExpenseDate() == null) continue;
			LocalDate d = e.getExpenseDate();
			if (d.isBefore(from) || d.isAfter(to)) continue;
			BigDecimal amt = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
			if (byWeek) {
				LocalDate key = d.with(wf.dayOfWeek(), 1);
				if (key.isBefore(from)) key = key.plusWeeks(1);
				byPeriodRaw.merge(key, amt, BigDecimal::add);
			} else {
				byPeriodRaw.merge(d, amt, BigDecimal::add);
			}
		}

		List<RevenueByMonthDto> result = new ArrayList<>();
		for (Map.Entry<LocalDate, BigDecimal> e : byPeriodRaw.entrySet()) {
			String label = byWeek
				? (e.getKey().format(WEEK_FMT) + " – " + e.getKey().plusDays(6).format(WEEK_FMT))
				: e.getKey().format(DAY_FMT);
			result.add(new RevenueByMonthDto(label, e.getValue()));
		}
		return result;
	}

	/** Подписи для графика «Оборудование по статусам». */
	public List<String> getChartEquipmentLabels() {
		return List.of("Свободно", "Занято", "Забронировано");
	}

	/** Значения для графика «Оборудование по статусам». */
	public List<Long> getChartEquipmentValues() {
		return List.of(
			getEquipmentFreeCount(),
			getEquipmentBusyCount(),
			getEquipmentReservedCount()
		);
	}

	/** Подписи для графика «Прокаты по статусам». */
	public List<String> getChartRentalLabels() {
		return List.of("Активные", "Ожидают доставки", "Завершённые", "Должники", "Отменённые", "Бронь", "Скоро должник");
	}

	/** Значения для графика «Прокаты по статусам». */
	public List<Long> getChartRentalValues() {
		return List.of(
			getActiveRentalsCount(),
			getAwaitingDeliveryRentalsCount(),
			getCompletedRentalsCount(),
			getDebtorsCount(),
			(long) rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.CANCELLED).size(),
			(long) rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.BOOKED).size(),
			(long) rentalRepository.findByStatusOrderByDateFromDesc(RentalStatus.SOON_DEBTOR).size()
		);
	}
}
