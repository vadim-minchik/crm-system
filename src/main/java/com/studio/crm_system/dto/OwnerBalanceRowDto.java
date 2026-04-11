package com.studio.crm_system.dto;

import java.math.BigDecimal;


public class OwnerBalanceRowDto {

	private Long equipmentId;
	private String serialNumber;
	private String modelTitle;
	private Long equipmentOwnerId;
	private Long equipmentOwnerVersion;
	private String ownerName;
	
	private BigDecimal ownershipPercent;
	
	private BigDecimal sharePercent;
	private BigDecimal accrued;
	private BigDecimal paid;
	private BigDecimal toPay;

	public Long getEquipmentId() { return equipmentId; }
	public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
	public String getSerialNumber() { return serialNumber; }
	public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
	public String getModelTitle() { return modelTitle; }
	public void setModelTitle(String modelTitle) { this.modelTitle = modelTitle; }
	public Long getEquipmentOwnerId() { return equipmentOwnerId; }
	public void setEquipmentOwnerId(Long equipmentOwnerId) { this.equipmentOwnerId = equipmentOwnerId; }
	public Long getEquipmentOwnerVersion() { return equipmentOwnerVersion; }
	public void setEquipmentOwnerVersion(Long equipmentOwnerVersion) { this.equipmentOwnerVersion = equipmentOwnerVersion; }
	public String getOwnerName() { return ownerName; }
	public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
	public BigDecimal getOwnershipPercent() { return ownershipPercent; }
	public void setOwnershipPercent(BigDecimal ownershipPercent) { this.ownershipPercent = ownershipPercent; }
	public BigDecimal getSharePercent() { return sharePercent; }
	public void setSharePercent(BigDecimal sharePercent) { this.sharePercent = sharePercent; }
	public BigDecimal getAccrued() { return accrued; }
	public void setAccrued(BigDecimal accrued) { this.accrued = accrued; }
	public BigDecimal getPaid() { return paid; }
	public void setPaid(BigDecimal paid) { this.paid = paid; }
	public BigDecimal getToPay() { return toPay; }
	public void setToPay(BigDecimal toPay) { this.toPay = toPay; }
}
