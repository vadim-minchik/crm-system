package com.studio.crm_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Бронь: номер телефона + свободное оборудование. При удалении брони запись удаляется, оборудование снова FREE.
 */
@Entity
@Table(name = "bookings")
public class Booking {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String phoneNumber;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "equipment_id", nullable = false)
	private Equipment equipment;

	/** По какое число и время действует бронь; после этого момента бронь автоматически удаляется, оборудование снова свободно. */
	@Column(nullable = false)
	private LocalDateTime dateTo;

	/** Комментарий к брони (необязательно). */
	@Column(length = 1000)
	private String comment;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getPhoneNumber() { return phoneNumber; }
	public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

	public Equipment getEquipment() { return equipment; }
	public void setEquipment(Equipment equipment) { this.equipment = equipment; }

	public LocalDateTime getDateTo() { return dateTo; }
	public void setDateTo(LocalDateTime dateTo) { this.dateTo = dateTo; }

	public String getComment() { return comment; }
	public void setComment(String comment) { this.comment = comment; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
