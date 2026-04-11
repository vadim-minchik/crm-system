package com.studio.crm_system.controller;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.ExpenseService;
import com.studio.crm_system.web.OptimisticLockSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/statistics/expenses")
public class ExpenseController {

	@Autowired
	private UserRepository userRepository;

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
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate expenseDate,
			@RequestParam BigDecimal amount,
			@RequestParam String description,
			@RequestParam(required = false) String category) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";
		if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
			return "redirect:/statistics?tab=expenses&error=invalid_amount";
		if (description == null || description.isBlank())
			return "redirect:/statistics?tab=expenses&error=description_required";

		expenseService.add(expenseDate, amount, description, category, user);
		return "redirect:/statistics?tab=expenses&success=expense_added#tabFullExpenses";
	}

	@PostMapping("/edit")
	public String edit(
			@RequestParam Long id,
			@RequestParam Long version,
			@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate expenseDate,
			@RequestParam BigDecimal amount,
			@RequestParam String description,
			@RequestParam(required = false) String category) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";
		if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
			return "redirect:/statistics?tab=expenses&error=invalid_amount#tabFullExpenses";
		if (description == null || description.isBlank())
			return "redirect:/statistics?tab=expenses&error=description_required#tabFullExpenses";

		var opt = expenseService.findById(id);
		if (opt.isEmpty())
			return "redirect:/statistics?tab=expenses&error=not_found#tabFullExpenses";
		if (OptimisticLockSupport.isStale(version, opt.get().getVersion()))
			return "redirect:/statistics?tab=expenses&error=stale_data#tabFullExpenses";
		expenseService.applyUpdate(opt.get(), expenseDate, amount, description, category);
		return "redirect:/statistics?tab=expenses&success=expense_updated#tabFullExpenses";
	}

	@PostMapping("/delete")
	public String delete(@RequestParam Long id, @RequestParam Long version) {
		if (getCurrentUser() == null) return "redirect:/login";
		var opt = expenseService.findById(id);
		if (opt.isEmpty())
			return "redirect:/statistics?tab=expenses&error=not_found#tabFullExpenses";
		if (OptimisticLockSupport.isStale(version, opt.get().getVersion()))
			return "redirect:/statistics?tab=expenses&error=stale_data#tabFullExpenses";
		expenseService.applySoftDelete(opt.get());
		return "redirect:/statistics?tab=expenses&success=expense_deleted#tabFullExpenses";
	}
}
