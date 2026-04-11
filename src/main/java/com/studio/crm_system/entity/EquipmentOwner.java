package com.studio.crm_system.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;


@Entity
@Table(name = "equipment_owners")
public class EquipmentOwner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "equipment_id", nullable = false)
	private Equipment equipment;

	@Column(nullable = false, length = 200)
	private String ownerName;

	
	@Column(name = "ownership_percent", precision = 6, scale = 2)
	private BigDecimal ownershipPercent;

	
	@Column(name = "rental_share_percent", nullable = false, precision = 6, scale = 2)
	private BigDecimal rentalSharePercent;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder = 0;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }

	public Equipment getEquipment() { return equipment; }
	public void setEquipment(Equipment equipment) { this.equipment = equipment; }

	public String getOwnerName() { return ownerName; }
	public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

	public BigDecimal getOwnershipPercent() {
		if (ownershipPercent != null) {
			return ownershipPercent;
		}
		return rentalSharePercent != null ? rentalSharePercent : new BigDecimal("100");
	}

	public void setOwnershipPercent(BigDecimal ownershipPercent) {
		this.ownershipPercent = ownershipPercent;
	}

	public BigDecimal getRentalSharePercent() { return rentalSharePercent; }
	public void setRentalSharePercent(BigDecimal rentalSharePercent) { this.rentalSharePercent = rentalSharePercent; }

	public int getSortOrder() { return sortOrder; }
	public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
