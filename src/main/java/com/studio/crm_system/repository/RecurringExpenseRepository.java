package com.studio.crm_system.repository;

import com.studio.crm_system.entity.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

	List<RecurringExpense> findByIsDeletedFalseOrderByStartDateDesc();
}
