package com.studio.crm_system.dto;

import java.math.BigDecimal;


public class RevenueByMonthDto {
	private final String monthLabel;
	private final BigDecimal total;

	public RevenueByMonthDto(String monthLabel, BigDecimal total) {
		this.monthLabel = monthLabel;
		this.total = total != null ? total : BigDecimal.ZERO;
	}

	public String getMonthLabel() { return monthLabel; }
	public BigDecimal getTotal() { return total; }
}
