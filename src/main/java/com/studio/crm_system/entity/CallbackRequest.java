package com.studio.crm_system.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "callback_requests")
public class CallbackRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(nullable = false, length = 50)
	private String phoneNumber;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "client_id")
	private Client client;

	@Column(length = 50)
	private String surname;

	@Column(length = 50)
	private String name;

	@Column(length = 50)
	private String patronymic;

	@Column(length = 2000)
	private String equipmentWish;

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "equipment_id")
	private Equipment equipment;

	private LocalDateTime dateFrom;

	private LocalDateTime dateTo;

	
	private LocalDateTime remindAt;

	@Column(length = 2000)
	private String comment;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "creator_user_id")
	private User creator;

	
	@Column(length = 100)
	private String createdBy;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPatronymic() {
		return patronymic;
	}

	public void setPatronymic(String patronymic) {
		this.patronymic = patronymic;
	}

	public String getEquipmentWish() {
		return equipmentWish;
	}

	public void setEquipmentWish(String equipmentWish) {
		this.equipmentWish = equipmentWish;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

	
	public String getEquipmentDisplayLabel() {
		if (equipment != null) {
			return equipment.getTitle() + " — S/N " + equipment.getSerialNumber();
		}
		if (equipmentWish != null && !equipmentWish.isBlank()) {
			return equipmentWish.trim();
		}
		return null;
	}

	public LocalDateTime getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(LocalDateTime dateFrom) {
		this.dateFrom = dateFrom;
	}

	public LocalDateTime getDateTo() {
		return dateTo;
	}

	public void setDateTo(LocalDateTime dateTo) {
		this.dateTo = dateTo;
	}

	public LocalDateTime getRemindAt() {
		return remindAt;
	}

	public void setRemindAt(LocalDateTime remindAt) {
		this.remindAt = remindAt;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

	
	public boolean isRemindUrgent() {
		if (remindAt == null) {
			return false;
		}
		LocalDate d = remindAt.toLocalDate();
		LocalDate today = LocalDate.now();
		return d.equals(today) || d.equals(today.plusDays(1));
	}

	
	public String getCreatorDisplaySurname() {
		if (creator != null && creator.getSurname() != null && !creator.getSurname().isBlank()) {
			return creator.getSurname().trim();
		}
		return createdBy != null ? createdBy : "";
	}

	
	public String getDisplayFio() {
		if (client != null) {
			String p = client.getPatronymic() != null ? client.getPatronymic() : "";
			return (client.getSurname() + " " + client.getName() + " " + p).trim();
		}
		StringBuilder sb = new StringBuilder();
		if (surname != null && !surname.isBlank())
			sb.append(surname.trim());
		if (name != null && !name.isBlank()) {
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(name.trim());
		}
		if (patronymic != null && !patronymic.isBlank()) {
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(patronymic.trim());
		}
		return sb.length() > 0 ? sb.toString() : "—";
	}
}
