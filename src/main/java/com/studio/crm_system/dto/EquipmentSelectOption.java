package com.studio.crm_system.dto;

import java.math.BigDecimal;

/**
 * Элемент списка выбора оборудования: название, серийник, статус (свободен / в прокате до … / забронирован).
 */
public class EquipmentSelectOption {

	private Long id;
	private String title;
	private String serialNumber;
	private String statusLabel;
	private BigDecimal pricePerDay;
	private BigDecimal pricePerWeek;
	private BigDecimal pricePerHour;
	private boolean free;

	public EquipmentSelectOption() {}

	public EquipmentSelectOption(Long id, String title, String serialNumber, String statusLabel,
	                             BigDecimal pricePerDay, BigDecimal pricePerWeek, BigDecimal pricePerHour, boolean free) {
		this.id = id;
		this.title = title;
		this.serialNumber = serialNumber;
		this.statusLabel = statusLabel;
		this.pricePerDay = pricePerDay;
		this.pricePerWeek = pricePerWeek;
		this.pricePerHour = pricePerHour;
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
	public BigDecimal getPricePerDay() { return pricePerDay; }
	public void setPricePerDay(BigDecimal pricePerDay) { this.pricePerDay = pricePerDay; }
	public BigDecimal getPricePerWeek() { return pricePerWeek; }
	public void setPricePerWeek(BigDecimal pricePerWeek) { this.pricePerWeek = pricePerWeek; }
	public BigDecimal getPricePerHour() { return pricePerHour; }
	public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }
	public boolean isFree() { return free; }
	public void setFree(boolean free) { this.free = free; }
}
