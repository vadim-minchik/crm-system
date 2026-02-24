package com.studio.crm_system.service;

import com.studio.crm_system.entity.Expense;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.ExpenseRepository;
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
		Expense e = new Expense();
		e.setExpenseDate(expenseDate);
		e.setAmount(amount != null ? amount : BigDecimal.ZERO);
		e.setDescription(description != null ? description.trim() : "");
		e.setCategory(category != null && !category.isBlank() ? category.trim() : null);
		e.setCreatedBy(createdBy);
		return expenseRepository.save(e);
	}

	@Transactional
	public boolean update(Long id, LocalDate expenseDate, BigDecimal amount, String description, String category) {
		Optional<Expense> opt = findById(id);
		if (opt.isEmpty()) return false;
		Expense e = opt.get();
		if (expenseDate != null) e.setExpenseDate(expenseDate);
		if (amount != null) e.setAmount(amount);
		if (description != null) e.setDescription(description.trim());
		e.setCategory(category != null && !category.isBlank() ? category.trim() : null);
		expenseRepository.save(e);
		return true;
	}

	@Transactional
	public boolean delete(Long id) {
		Optional<Expense> opt = findById(id);
		if (opt.isEmpty()) return false;
		Expense e = opt.get();
		e.setIsDeleted(true);
		expenseRepository.save(e);
		return true;
	}
}
