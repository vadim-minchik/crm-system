package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

	List<Expense> findByIsDeletedFalseOrderByExpenseDateDesc();
}
