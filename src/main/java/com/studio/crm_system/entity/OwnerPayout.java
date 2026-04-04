package com.studio.crm_system.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "owner_payouts")
public class OwnerPayout {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private Long version;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "equipment_id", nullable = false)
	private Equipment equipment;

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "equipment_owner_id")
	private EquipmentOwner equipmentOwner;

	@Column(nullable = false, length = 200)
	private String payeeName;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false)
	private LocalDateTime paidAt;

	@Column(length = 500)
	private String note;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recorded_by_user_id")
	private User recordedBy;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }

	public Equipment getEquipment() { return equipment; }
	public void setEquipment(Equipment equipment) { this.equipment = equipment; }

	public EquipmentOwner getEquipmentOwner() { return equipmentOwner; }
	public void setEquipmentOwner(EquipmentOwner equipmentOwner) { this.equipmentOwner = equipmentOwner; }

	public String getPayeeName() { return payeeName; }
	public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

	public BigDecimal getAmount() { return amount; }
	public void setAmount(BigDecimal amount) { this.amount = amount; }

	public LocalDateTime getPaidAt() { return paidAt; }
	public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

	public String getNote() { return note; }
	public void setNote(String note) { this.note = note; }

	public User getRecordedBy() { return recordedBy; }
	public void setRecordedBy(User recordedBy) { this.recordedBy = recordedBy; }
}
