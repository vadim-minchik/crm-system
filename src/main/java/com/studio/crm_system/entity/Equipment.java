package com.studio.crm_system.entity;

import com.studio.crm_system.enums.EquipmentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "equipment")
@NamedEntityGraphs({
		@NamedEntityGraph(
				name = "Equipment.withOwners",
				attributeNodes = {
						@NamedAttributeNode("owners"),
						@NamedAttributeNode("point")
				}
		),
		@NamedEntityGraph(
				name = "Equipment.forUnitDetail",
				attributeNodes = {
						@NamedAttributeNode("owners"),
						@NamedAttributeNode("point"),
						@NamedAttributeNode(value = "toolName", subgraph = "ToolName.forUnitDetail")
				},
				subgraphs = {
						@NamedSubgraph(
								name = "ToolName.forUnitDetail",
								attributeNodes = {
										@NamedAttributeNode(value = "category", subgraph = "Category.forUnitDetail")
								}
						),
						@NamedSubgraph(
								name = "Category.forUnitDetail",
								attributeNodes = @NamedAttributeNode("preCategory")
						)
				}
		)
})
public class Equipment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private Long version;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "tool_name_id", nullable = false)
	private ToolName toolName;

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "point_id")
	private Point point;

	
	@Column(nullable = false)
	private String serialNumber;

	@Column(nullable = false)
	private Integer condition = 10;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal baseValue;

	
	@Column(name = "price_first_day", nullable = false, precision = 10, scale = 2)
	private BigDecimal priceFirstDay;

	
	@Column(name = "price_second_day", nullable = false, precision = 10, scale = 2)
	private BigDecimal priceSecondDay;

	
	@Column(name = "price_subsequent_days", nullable = false, precision = 10, scale = 2)
	private BigDecimal priceSubsequentDays;

	
	@Column(name = "price_first_month", nullable = false, precision = 10, scale = 2)
	private BigDecimal priceFirstMonth;

	
	@Column(name = "price_second_month", nullable = false, precision = 10, scale = 2)
	private BigDecimal priceSecondMonth;

	
	@Column(name = "price_subsequent_months", nullable = false, precision = 10, scale = 2)
	private BigDecimal priceSubsequentMonths;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private EquipmentStatus status = EquipmentStatus.FREE;

	@Column(nullable = false)
	private Boolean isDeleted = false;

	@OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<EquipmentOwner> owners = new ArrayList<>();

	public String getTitle() {
		return toolName != null ? toolName.getName() : "Неизвестно";
	}

	public Boolean getIsAvailable() {
		return status == EquipmentStatus.FREE;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }

	public ToolName getToolName() { return toolName; }
	public void setToolName(ToolName toolName) { this.toolName = toolName; }

	public Point getPoint() { return point; }
	public void setPoint(Point point) { this.point = point; }

	public String getSerialNumber() { return serialNumber; }
	public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

	public Integer getCondition() { return condition; }
	public void setCondition(Integer condition) { this.condition = condition; }

	public BigDecimal getBaseValue() { return baseValue; }
	public void setBaseValue(BigDecimal baseValue) { this.baseValue = baseValue; }

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

	public EquipmentStatus getStatus() { return status; }
	public void setStatus(EquipmentStatus status) { this.status = status; }

	public Boolean getIsDeleted() { return isDeleted; }
	public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

	public List<EquipmentOwner> getOwners() { return owners; }
	public void setOwners(List<EquipmentOwner> owners) { this.owners = owners; }
}
