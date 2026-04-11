package com.studio.crm_system.controller;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.dto.OwnerBalanceRowDto;
import com.studio.crm_system.service.ExpenseService;
import com.studio.crm_system.service.OwnerShareService;
import com.studio.crm_system.service.RecurringExpenseService;
import com.studio.crm_system.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/statistics")
public class StatisticsController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private StatisticsService statisticsService;

	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = (principal instanceof UserDetails)
				? ((UserDetails) principal).getUsername()
				: principal.toString();
		return userRepository.findByLoginAndIsDeletedFalse(username).orElse(null);
	}

	@Autowired
	private ExpenseService expenseService;

	@Autowired
	private RecurringExpenseService recurringExpenseService;

	@Autowired
	private OwnerShareService ownerShareService;

	@GetMapping
	public String showStatistics(
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate dateFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate dateTo,
			@RequestParam(required = false) String tab,
			Model model) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		model.addAttribute("currentUser", user);
		model.addAttribute("username", user.getLogin());
		model.addAttribute("currentUserRole", user.getRole());

		model.addAttribute("clientsCount", statisticsService.getClientsCount());
		model.addAttribute("equipmentTotal", statisticsService.getEquipmentTotalCount());
		model.addAttribute("equipmentFree", statisticsService.getEquipmentFreeCount());
		model.addAttribute("equipmentBusy", statisticsService.getEquipmentBusyCount());
		model.addAttribute("equipmentReserved", statisticsService.getEquipmentReservedCount());
		long eqTotal = statisticsService.getEquipmentTotalCount();
		if (eqTotal > 0) {
			model.addAttribute("equipmentFreePct", Math.round(100.0 * statisticsService.getEquipmentFreeCount() / eqTotal));
			model.addAttribute("equipmentBusyPct", Math.round(100.0 * statisticsService.getEquipmentBusyCount() / eqTotal));
			model.addAttribute("equipmentReservedPct", Math.round(100.0 * statisticsService.getEquipmentReservedCount() / eqTotal));
		} else {
			model.addAttribute("equipmentFreePct", 0);
			model.addAttribute("equipmentBusyPct", 0);
			model.addAttribute("equipmentReservedPct", 0);
		}
		model.addAttribute("activeRentalsCount", statisticsService.getActiveRentalsCount());
		model.addAttribute("completedRentalsCount", statisticsService.getCompletedRentalsCount());
		model.addAttribute("debtorsCount", statisticsService.getDebtorsCount());
		model.addAttribute("bookingsCount", statisticsService.getBookingsCount());
		model.addAttribute("staffCount", statisticsService.getStaffCount());
		model.addAttribute("pointsCount", statisticsService.getPointsCount());

		BigDecimal totalRevenue = statisticsService.getTotalRevenue();
		model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

		model.addAttribute("clientsList", statisticsService.getClientsList());
		model.addAttribute("freeEquipmentList", statisticsService.getFreeEquipmentList());
		model.addAttribute("busyEquipmentList", statisticsService.getBusyEquipmentList());
		model.addAttribute("reservedEquipmentList", statisticsService.getReservedEquipmentList());
		model.addAttribute("allEquipmentList", statisticsService.getAllEquipmentList());
		model.addAttribute("activeRentalsList", statisticsService.getActiveRentalsList());
		model.addAttribute("completedRentalsList", statisticsService.getCompletedRentalsList());
		model.addAttribute("debtorsList", statisticsService.getDebtorsList());
		model.addAttribute("bookingsList", statisticsService.getBookingsList());
		model.addAttribute("staffList", statisticsService.getStaffList());
		model.addAttribute("pointsList", statisticsService.getPointsList());
		model.addAttribute("allRentalsList", statisticsService.getAllRentalsList());

		boolean useRange = dateFrom != null && dateTo != null && !dateFrom.isAfter(dateTo);
		List<com.studio.crm_system.dto.RevenueByMonthDto> revenueByMonth = useRange
				? statisticsService.getRevenueByDateRange(dateFrom, dateTo)
				: statisticsService.getRevenueByMonthLast12();
		List<BigDecimal> expenseValues = useRange
				? statisticsService.getExpensesByDateRange(dateFrom, dateTo).stream().map(com.studio.crm_system.dto.RevenueByMonthDto::getTotal).collect(Collectors.toList())
				: statisticsService.getExpensesByMonthLast12();
		model.addAttribute("revenueByMonth", revenueByMonth);
		model.addAttribute("revenueDateFrom", dateFrom);
		model.addAttribute("revenueDateTo", dateTo);
		model.addAttribute("revenueChartLabels", revenueByMonth.stream().map(com.studio.crm_system.dto.RevenueByMonthDto::getMonthLabel).collect(Collectors.toList()));
		model.addAttribute("revenueChartValues", revenueByMonth.stream().map(d -> d.getTotal().doubleValue()).collect(Collectors.toList()));
		model.addAttribute("expenseChartValues", expenseValues.stream().map(BigDecimal::doubleValue).collect(Collectors.toList()));
		BigDecimal chartPeriodRevenue = revenueByMonth.stream().map(com.studio.crm_system.dto.RevenueByMonthDto::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal chartPeriodExpenses = expenseValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		model.addAttribute("chartPeriodRevenue", chartPeriodRevenue);
		model.addAttribute("chartPeriodExpenses", chartPeriodExpenses);
		model.addAttribute("chartEquipmentLabels", statisticsService.getChartEquipmentLabels());
		model.addAttribute("chartEquipmentValues", statisticsService.getChartEquipmentValues());
		model.addAttribute("chartRentalLabels", statisticsService.getChartRentalLabels());
		model.addAttribute("chartRentalValues", statisticsService.getChartRentalValues());

		expenseService.generateRecurringExpensesUpTo(LocalDate.now());
		model.addAttribute("expensesList", expenseService.findAll());
		model.addAttribute("totalExpenses", expenseService.getTotalExpenses());
		model.addAttribute("recurringExpensesList", recurringExpenseService.findAll());
		String statsTab = "expenses".equals(tab) ? "expenses" : ("owners".equals(tab) ? "owners" : null);
		model.addAttribute("activeFullStatsTab", statsTab);

		List<OwnerBalanceRowDto> ownerBalanceRows = ownerShareService.buildAllOwnerBalanceRows();
		model.addAttribute("ownerBalanceRows", ownerBalanceRows);
		BigDecimal ownersTotalAccrued = ownerBalanceRows.stream()
				.map(OwnerBalanceRowDto::getAccrued)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal ownersTotalPaid = ownerBalanceRows.stream()
				.map(OwnerBalanceRowDto::getPaid)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal ownersTotalToPay = ownerBalanceRows.stream()
				.map(OwnerBalanceRowDto::getToPay)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		model.addAttribute("ownersTotalAccrued", ownersTotalAccrued);
		model.addAttribute("ownersTotalPaid", ownersTotalPaid);
		model.addAttribute("ownersTotalToPay", ownersTotalToPay);
		model.addAttribute("ownerRecentPayouts", ownerShareService.recentPayoutsAll(40));

		return "html/statistics";
	}
}
