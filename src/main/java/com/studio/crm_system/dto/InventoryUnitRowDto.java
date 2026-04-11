package com.studio.crm_system.dto;

import java.math.BigDecimal;


public class InventoryUnitRowDto {

	private Long id;
	private Long version;
	private String serialNumber;
	private Integer condition;
	private BigDecimal priceFirstDay;
	private BigDecimal priceSecondDay;
	private BigDecimal priceSubsequentDays;
	private BigDecimal priceFirstMonth;
	private BigDecimal priceSecondMonth;
	private BigDecimal priceSubsequentMonths;
	private BigDecimal baseValue;
	private String status;       
	private String statusLabel;  
	private boolean inBooking;
	private Long pointId;
	private String pointName;
	
	private String ownersSummary;
	
	private String ownersJson;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }
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
