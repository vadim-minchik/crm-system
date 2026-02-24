package com.studio.crm_system.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Расход компании — учёт затрат.
 */
@Entity
@Table(name = "expenses")
public class Expense {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false)
	private LocalDate expenseDate;

	@Column(nullable = false, length = 500)
	private String description;

	@Column(length = 100)
	private String category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_id")
	private User createdBy;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(nullable = false)
	private Boolean isDeleted = false;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public BigDecimal getAmount() { return amount; }
	public void setAmount(BigDecimal amount) { this.amount = amount; }

	public LocalDate getExpenseDate() { return expenseDate; }
	public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public String getCategory() { return category; }
	public void setCategory(String category) { this.category = category; }

	public User getCreatedBy() { return createdBy; }
	public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

	public Boolean getIsDeleted() { return isDeleted; }
	public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
}
