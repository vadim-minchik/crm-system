package com.studio.crm_system.entity;

import com.studio.crm_system.enums.EquipmentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "equipment")
public class Equipment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "tool_name_id", nullable = false)
	private ToolName toolName;

	@Column(nullable = false, unique = true)
	private String serialNumber;

	@Column(nullable = false)
	private Integer condition = 10;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal baseValue;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal pricePerHour;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal pricePerDay;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal pricePerWeek;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal pricePerMonth;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private EquipmentStatus status = EquipmentStatus.FREE;

	@Column(nullable = false)
	private Boolean isDeleted = false;

	public String getTitle() {
		return toolName != null ? toolName.getName() : "Неизвестно";
	}

	public Boolean getIsAvailable() {
		return status == EquipmentStatus.FREE;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public ToolName getToolName() { return toolName; }
	public void setToolName(ToolName toolName) { this.toolName = toolName; }

	public String getSerialNumber() { return serialNumber; }
	public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

	public Integer getCondition() { return condition; }
	public void setCondition(Integer condition) { this.condition = condition; }

	public BigDecimal getBaseValue() { return baseValue; }
	public void setBaseValue(BigDecimal baseValue) { this.baseValue = baseValue; }

	public BigDecimal getPricePerHour() { return pricePerHour; }
	public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }

	public BigDecimal getPricePerDay() { return pricePerDay; }
	public void setPricePerDay(BigDecimal pricePerDay) { this.pricePerDay = pricePerDay; }

	public BigDecimal getPricePerWeek() { return pricePerWeek; }
	public void setPricePerWeek(BigDecimal pricePerWeek) { this.pricePerWeek = pricePerWeek; }

	public BigDecimal getPricePerMonth() { return pricePerMonth; }
	public void setPricePerMonth(BigDecimal pricePerMonth) { this.pricePerMonth = pricePerMonth; }

	public EquipmentStatus getStatus() { return status; }
	public void setStatus(EquipmentStatus status) { this.status = status; }

	public Boolean getIsDeleted() { return isDeleted; }
	public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
}
