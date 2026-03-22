package com.studio.crm_system.service;

import com.studio.crm_system.entity.RecurringExpense;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.enums.PeriodType;
import com.studio.crm_system.repository.RecurringExpenseRepository;
import com.studio.crm_system.security.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RecurringExpenseService {

	@Autowired
	private RecurringExpenseRepository recurringExpenseRepository;

	@Autowired
	private InputValidator inputValidator;

	public List<RecurringExpense> findAll() {
		return recurringExpenseRepository.findByIsDeletedFalseOrderByStartDateDesc();
	}

	public Optional<RecurringExpense> findById(Long id) {
		return recurringExpenseRepository.findById(id)
				.filter(r -> !r.getIsDeleted());
	}

	/**
	 * Валидация: сумма > 0, описание не пустое, startDate <= endDate.
	 */
	public String validate(BigDecimal amount, String description, String category, LocalDate startDate, LocalDate endDate) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
			return "Сумма должна быть больше нуля.";
		if (description == null || description.isBlank())
			return "Укажите описание.";
		if (!inputValidator.isSafeInput(description))
			return "Недопустимые символы в описании.";
		if (category != null && !category.isBlank() && !inputValidator.isSafeInput(category))
			return "Недопустимые символы в категории.";
		if (startDate == null)
			return "Укажите дату начала.";
		if (endDate == null)
			return "Укажите дату окончания.";
		if (startDate.isAfter(endDate))
			return "Дата начала не может быть позже даты окончания.";
		return null;
	}

	@Transactional
	public RecurringExpense add(BigDecimal amount, String description, String category,
			LocalDate startDate, LocalDate endDate, PeriodType periodType, User createdBy) {
		RecurringExpense r = new RecurringExpense();
		r.setAmount(amount != null ? amount : BigDecimal.ZERO);
		r.setDescription(description != null ? description.trim() : "");
		r.setCategory(category != null && !category.isBlank() ? category.trim() : null);
		r.setStartDate(startDate);
		r.setEndDate(endDate);
		r.setPeriodType(periodType != null ? periodType : PeriodType.MONTHLY);
		r.setCreatedBy(createdBy);
		return recurringExpenseRepository.save(r);
	}

	@Transactional
	public boolean update(Long id, BigDecimal amount, String description, String category,
			LocalDate startDate, LocalDate endDate, PeriodType periodType) {
		Optional<RecurringExpense> opt = findById(id);
		if (opt.isEmpty()) return false;
		applyUpdate(opt.get(), amount, description, category, startDate, endDate, periodType);
		return true;
	}

	@Transactional
	public void applyUpdate(RecurringExpense r, BigDecimal amount, String description, String category,
			LocalDate startDate, LocalDate endDate, PeriodType periodType) {
		if (amount != null) r.setAmount(amount);
		if (description != null) r.setDescription(description.trim());
		r.setCategory(category != null && !category.isBlank() ? category.trim() : null);
		if (startDate != null) r.setStartDate(startDate);
		if (endDate != null) r.setEndDate(endDate);
		if (periodType != null) r.setPeriodType(periodType);
		recurringExpenseRepository.save(r);
	}

	@Transactional
	public boolean delete(Long id) {
		Optional<RecurringExpense> opt = findById(id);
		if (opt.isEmpty()) return false;
		applySoftDelete(opt.get());
		return true;
	}

	@Transactional
	public void applySoftDelete(RecurringExpense r) {
		r.setIsDeleted(true);
		recurringExpenseRepository.save(r);
	}
}
