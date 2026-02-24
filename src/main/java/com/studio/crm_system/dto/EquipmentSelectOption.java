package com.studio.crm_system.dto;

import java.math.BigDecimal;

public class EquipmentSelectOption {

	private Long id;
	private String title;
	private String serialNumber;
	private String statusLabel;
	private BigDecimal priceFirstDay;
	private BigDecimal priceSecondDay;
	private BigDecimal priceSubsequentDays;
	private boolean free;

	public EquipmentSelectOption() {}

	public EquipmentSelectOption(Long id, String title, String serialNumber, String statusLabel,
	                             BigDecimal priceFirstDay, BigDecimal priceSecondDay, BigDecimal priceSubsequentDays, boolean free) {
		this.id = id;
		this.title = title;
		this.serialNumber = serialNumber;
		this.statusLabel = statusLabel;
		this.priceFirstDay = priceFirstDay;
		this.priceSecondDay = priceSecondDay;
		this.priceSubsequentDays = priceSubsequentDays;
		this.free = free;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getSerialNumber() { return serialNumber; }
	public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
	public String getStatusLabel() { return statusLabel; }
	public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
	public BigDecimal getPriceFirstDay() { return priceFirstDay; }
	public void setPriceFirstDay(BigDecimal priceFirstDay) { this.priceFirstDay = priceFirstDay; }
	public BigDecimal getPriceSecondDay() { return priceSecondDay; }
	public void setPriceSecondDay(BigDecimal priceSecondDay) { this.priceSecondDay = priceSecondDay; }
	public BigDecimal getPriceSubsequentDays() { return priceSubsequentDays; }
	public void setPriceSubsequentDays(BigDecimal priceSubsequentDays) { this.priceSubsequentDays = priceSubsequentDays; }
	public boolean isFree() { return free; }
	public void setFree(boolean free) { this.free = free; }
}
