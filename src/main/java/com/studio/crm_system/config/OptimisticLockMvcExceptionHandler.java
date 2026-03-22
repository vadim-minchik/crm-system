package com.studio.crm_system.config;

import com.studio.crm_system.entity.*;
import jakarta.persistence.OptimisticLockException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Редирект с понятной ошибкой при конфликте версий (два пользователя меняют одну запись).
 */
@ControllerAdvice
@Order(100)
public class OptimisticLockMvcExceptionHandler {

	@ExceptionHandler({ ObjectOptimisticLockingFailureException.class, OptimisticLockException.class })
	public String handleJpaOptimisticLock(RuntimeException ex) {
		Class<?> entityClass = null;
		Object id = null;
		if (ex instanceof ObjectOptimisticLockingFailureException ool) {
			entityClass = ool.getPersistentClass();
			id = ool.getIdentifier();
		}
		return redirectForEntity(entityClass, id);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public String handleSpringDaoOptimisticLock(OptimisticLockingFailureException ex) {
		if (ex instanceof ObjectOptimisticLockingFailureException ool) {
			return handleJpaOptimisticLock(ool);
		}
		return "redirect:/staff?error=stale_data";
	}

	private static String redirectForEntity(Class<?> c, Object id) {
		String idStr = id != null ? id.toString() : null;
		if (c == null) {
			return "redirect:/staff?error=stale_data";
		}
		if (Client.class.equals(c)) {
			return idStr != null ? "redirect:/clients/" + idStr + "?error=stale_data" : "redirect:/clients?error=stale_data";
		}
		if (Rental.class.equals(c)) {
			return idStr != null ? "redirect:/rentals/" + idStr + "?error=stale_data" : "redirect:/rentals?error=stale_data";
		}
		if (Equipment.class.equals(c)) {
			return "redirect:/inventory?error=stale_data";
		}
		if (User.class.equals(c)) {
			return "redirect:/staff?error=stale_data";
		}
		if (Point.class.equals(c)) {
			return "redirect:/points?error=stale_data";
		}
		if (Booking.class.equals(c)) {
			return idStr != null ? "redirect:/rentals/booking/" + idStr + "?error=stale_data" : "redirect:/rentals?error=stale_data";
		}
		if (DocumentTemplate.class.equals(c)) {
			return "redirect:/documents?error=stale_data";
		}
		if (Expense.class.equals(c) || RecurringExpense.class.equals(c)) {
			return "redirect:/statistics?tab=expenses&error=stale_data#tabFullExpenses";
		}
		if (PreCategory.class.equals(c)) {
			return "redirect:/inventory?error=stale_data";
		}
		if (Category.class.equals(c) || ToolName.class.equals(c)) {
			return "redirect:/inventory?error=stale_data";
		}
		if (ClientReview.class.equals(c)) {
			return "redirect:/clients?error=stale_data";
		}
		if (EquipmentOwner.class.equals(c) || OwnerPayout.class.equals(c)) {
			return "redirect:/inventory?error=stale_data";
		}
		return "redirect:/staff?error=stale_data";
	}
}
