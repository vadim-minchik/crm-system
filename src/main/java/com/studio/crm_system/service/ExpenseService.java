package com.studio.crm_system.service;

import com.studio.crm_system.entity.Expense;
import com.studio.crm_system.entity.RecurringExpense;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.enums.PeriodType;
import com.studio.crm_system.repository.ExpenseRepository;
import com.studio.crm_system.repository.RecurringExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

	@Autowired
	private ExpenseRepository expenseRepository;

	@Autowired
	private RecurringExpenseRepository recurringExpenseRepository;

	public List<Expense> findAll() {
		return expenseRepository.findByIsDeletedFalseOrderByExpenseDateDesc();
	}

	public Optional<Expense> findById(Long id) {
		return expenseRepository.findById(id)
				.filter(e -> !e.getIsDeleted());
	}

	public BigDecimal getTotalExpenses() {
		return expenseRepository.findByIsDeletedFalseOrderByExpenseDateDesc().stream()
				.map(Expense::getAmount)
				.filter(a -> a != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Transactional
	public Expense add(LocalDate expenseDate, BigDecimal amount, String description, String category, User createdBy) {
		return add(expenseDate, amount, description, category, createdBy, null);
	}

	@Transactional
	public Expense add(LocalDate expenseDate, BigDecimal amount, String description, String category, User createdBy, Long recurringSourceId) {
		Expense e = new Expense();
		e.setExpenseDate(expenseDate);
		e.setAmount(amount != null ? amount : BigDecimal.ZERO);
		e.setDescription(description != null ? description.trim() : "");
		e.setCategory(category != null && !category.isBlank() ? category.trim() : null);
		e.setCreatedBy(createdBy);
		e.setRecurringSourceId(recurringSourceId);
		return expenseRepository.save(e);
	}

	/**
	 * Создаёт записи расходов по всем активным шаблонам повторяющихся расходов
	 * для периодов от startDate до min(endDate, endDate шаблона).
	 */
	@Transactional
	public void generateRecurringExpensesUpTo(LocalDate endDate) {
		if (endDate == null) return;
		List<RecurringExpense> templates = recurringExpenseRepository.findByIsDeletedFalseOrderByStartDateDesc();
		for (RecurringExpense r : templates) {
			if (r.getStartDate() == null || r.getEndDate() == null || r.getStartDate().isAfter(r.getEndDate()))
				continue;
			LocalDate limit = r.getEndDate().isAfter(endDate) ? endDate : r.getEndDate();
			LocalDate date = r.getStartDate();
			while (!date.isAfter(limit)) {
				if (!expenseRepository.existsByRecurringSourceIdAndExpenseDateAndIsDeletedFalse(r.getId(), date)) {
					User creator = r.getCreatedBy();
					add(date, r.getAmount(), r.getDescription(), r.getCategory(), creator != null ? creator : null, r.getId());
				}
				date = switch (r.getPeriodType() != null ? r.getPeriodType() : PeriodType.MONTHLY) {
					case DAILY -> date.plusDays(1);
					case WEEKLY -> date.plusWeeks(1);
					case MONTHLY -> date.plusMonths(1);
				};
			}
		}
	}

	@Transactional
	public boolean update(Long id, LocalDate expenseDate, BigDecimal amount, String description, String category) {
		Optional<Expense> opt = findById(id);
		if (opt.isEmpty()) return false;
		applyUpdate(opt.get(), expenseDate, amount, description, category);
		return true;
	}

	@Transactional
	public void applyUpdate(Expense e, LocalDate expenseDate, BigDecimal amount, String description, String category) {
		if (expenseDate != null) e.setExpenseDate(expenseDate);
		if (amount != null) e.setAmount(amount);
		if (description != null) e.setDescription(description.trim());
		e.setCategory(category != null && !category.isBlank() ? category.trim() : null);
		expenseRepository.save(e);
	}

	@Transactional
	public boolean delete(Long id) {
		Optional<Expense> opt = findById(id);
		if (opt.isEmpty()) return false;
		applySoftDelete(opt.get());
		return true;
	}

	@Transactional
	public void applySoftDelete(Expense e) {
		e.setIsDeleted(true);
		expenseRepository.save(e);
	}
}
