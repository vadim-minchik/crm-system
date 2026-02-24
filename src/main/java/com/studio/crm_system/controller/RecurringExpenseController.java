package com.studio.crm_system.controller;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.enums.PeriodType;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.ExpenseService;
import com.studio.crm_system.service.RecurringExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/statistics/expenses/recurring")
public class RecurringExpenseController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RecurringExpenseService recurringExpenseService;

	@Autowired
	private ExpenseService expenseService;

	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = (principal instanceof UserDetails)
				? ((UserDetails) principal).getUsername()
				: principal.toString();
		return userRepository.findByLogin(username).orElse(null);
	}

	@PostMapping("/add")
	public String add(
			@RequestParam BigDecimal amount,
			@RequestParam String description,
			@RequestParam(required = false) String category,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
			@RequestParam PeriodType periodType,
			RedirectAttributes redirectAttributes) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		String err = recurringExpenseService.validate(amount, description, category, startDate, endDate);
		if (err != null) {
			redirectAttributes.addFlashAttribute("recurringValidationError", err);
			return "redirect:/statistics?tab=expenses&error=recurring_validation#tabFullExpenses";
		}

		recurringExpenseService.add(amount, description, category, startDate, endDate, periodType, user);
		expenseService.generateRecurringExpensesUpTo(LocalDate.now());
		return "redirect:/statistics?tab=expenses&success=recurring_added#tabFullExpenses";
	}

	@PostMapping("/edit")
	public String edit(
			@RequestParam Long id,
			@RequestParam BigDecimal amount,
			@RequestParam String description,
			@RequestParam(required = false) String category,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
			@RequestParam PeriodType periodType,
			RedirectAttributes redirectAttributes) {
		if (getCurrentUser() == null) return "redirect:/login";

		String err = recurringExpenseService.validate(amount, description, category, startDate, endDate);
		if (err != null) {
			redirectAttributes.addFlashAttribute("recurringValidationError", err);
			return "redirect:/statistics?tab=expenses&error=recurring_validation#tabFullExpenses";
		}

		if (!recurringExpenseService.update(id, amount, description, category, startDate, endDate, periodType))
			return "redirect:/statistics?tab=expenses&error=not_found#tabFullExpenses";
		return "redirect:/statistics?tab=expenses&success=recurring_updated#tabFullExpenses";
	}

	@PostMapping("/delete")
	public String delete(@RequestParam Long id) {
		if (getCurrentUser() == null) return "redirect:/login";
		recurringExpenseService.delete(id);
		return "redirect:/statistics?tab=expenses&success=recurring_deleted#tabFullExpenses";
	}
}
