package com.studio.crm_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OwnerAccrualLineDto {

	private Long rentalId;
	private LocalDateTime rentalDateTo;
	private String clientShort;
	private BigDecimal lineAmount;

	public Long getRentalId() { return rentalId; }
	public void setRentalId(Long rentalId) { this.rentalId = rentalId; }
	public LocalDateTime getRentalDateTo() { return rentalDateTo; }
	public void setRentalDateTo(LocalDateTime rentalDateTo) { this.rentalDateTo = rentalDateTo; }
	public String getClientShort() { return clientShort; }
	public void setClientShort(String clientShort) { this.clientShort = clientShort; }
	public BigDecimal getLineAmount() { return lineAmount; }
	public void setLineAmount(BigDecimal lineAmount) { this.lineAmount = lineAmount; }
}
