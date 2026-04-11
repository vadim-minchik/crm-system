package com.studio.crm_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(nullable = false, length = 50)
	private String phoneNumber;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "booking_equipment",
			joinColumns = @JoinColumn(name = "booking_id"),
			inverseJoinColumns = @JoinColumn(name = "equipment_id"))
	private List<Equipment> equipmentList = new ArrayList<>();

	@Column
	private LocalDateTime dateFrom;

	@Column(nullable = false)
	private LocalDateTime dateTo;

	@Column(length = 1000)
	private String comment;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }

	public String getPhoneNumber() { return phoneNumber; }
	public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

	public List<Equipment> getEquipmentList() {
		return equipmentList != null ? equipmentList : Collections.emptyList();
	}
	public void setEquipmentList(List<Equipment> equipmentList) {
		this.equipmentList = equipmentList != null ? equipmentList : new ArrayList<>();
	}

	public String getEquipmentDisplayString() {
		List<Equipment> list = getEquipmentList();
		if (list == null || list.isEmpty()) return "—";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			Equipment eq = list.get(i);
			if (i > 0) sb.append(", ");
			sb.append(eq != null ? (eq.getTitle() + " — S/N " + eq.getSerialNumber()) : "—");
		}
		return sb.toString();
	}

	public LocalDateTime getDateFrom() { return dateFrom; }
	public void setDateFrom(LocalDateTime dateFrom) { this.dateFrom = dateFrom; }

	public LocalDateTime getDateTo() { return dateTo; }
	public void setDateTo(LocalDateTime dateTo) { this.dateTo = dateTo; }

	public String getComment() { return comment; }
	public void setComment(String comment) { this.comment = comment; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
