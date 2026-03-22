package com.studio.crm_system.dto;

import java.math.BigDecimal;

/**
 * Одна строка таблицы экземпляров на странице склада (для бесконечной подгрузки).
 */
public class InventoryUnitRowDto {

	private Long id;
	private String serialNumber;
	private Integer condition;
	private BigDecimal priceFirstDay;
	private BigDecimal priceSecondDay;
	private BigDecimal priceSubsequentDays;
	private BigDecimal priceFirstMonth;
	private BigDecimal priceSecondMonth;
	private BigDecimal priceSubsequentMonths;
	private BigDecimal baseValue;
	private String status;       // FREE, BUSY, RESERVED
	private String statusLabel;  // Свободно, Занят, Забронирован
	private boolean inBooking;
	private Long pointId;
	private String pointName;
	/** Краткая строка для таблицы: «Иванов 50% · Петров 50%». */
	private String ownersSummary;
	/** JSON [{name, percent}] для формы редактирования. */
	private String ownersJson;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getSerialNumber() { return serialNumber; }
	public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
	public Integer getCondition() { return condition; }
	public void setCondition(Integer condition) { this.condition = condition; }
	public BigDecimal getPriceFirstDay() { return priceFirstDay; }
	public void setPriceFirstDay(BigDecimal priceFirstDay) { this.priceFirstDay = priceFirstDay; }
	public BigDecimal getPriceSecondDay() { return priceSecondDay; }
	public void setPriceSecondDay(BigDecimal priceSecondDay) { this.priceSecondDay = priceSecondDay; }
	public BigDecimal getPriceSubsequentDays() { return priceSubsequentDays; }
	public void setPriceSubsequentDays(BigDecimal priceSubsequentDays) { this.priceSubsequentDays = priceSubsequentDays; }
	public BigDecimal getPriceFirstMonth() { return priceFirstMonth; }
	public void setPriceFirstMonth(BigDecimal priceFirstMonth) { this.priceFirstMonth = priceFirstMonth; }
	public BigDecimal getPriceSecondMonth() { return priceSecondMonth; }
	public void setPriceSecondMonth(BigDecimal priceSecondMonth) { this.priceSecondMonth = priceSecondMonth; }
	public BigDecimal getPriceSubsequentMonths() { return priceSubsequentMonths; }
	public void setPriceSubsequentMonths(BigDecimal priceSubsequentMonths) { this.priceSubsequentMonths = priceSubsequentMonths; }
	public BigDecimal getBaseValue() { return baseValue; }
	public void setBaseValue(BigDecimal baseValue) { this.baseValue = baseValue; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public String getStatusLabel() { return statusLabel; }
	public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
	public boolean isInBooking() { return inBooking; }
	public void setInBooking(boolean inBooking) { this.inBooking = inBooking; }
	public Long getPointId() { return pointId; }
	public void setPointId(Long pointId) { this.pointId = pointId; }
	public String getPointName() { return pointName; }
	public void setPointName(String pointName) { this.pointName = pointName; }
	public String getOwnersSummary() { return ownersSummary; }
	public void setOwnersSummary(String ownersSummary) { this.ownersSummary = ownersSummary; }
	public String getOwnersJson() { return ownersJson; }
	public void setOwnersJson(String ownersJson) { this.ownersJson = ownersJson; }
}
