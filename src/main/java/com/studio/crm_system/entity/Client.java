package com.studio.crm_system.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "clients")
public class Client {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// ===== ФИО =====
	@Column(nullable = false)
	private String surname;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String patronymic;

	// ===== ПАСПОРТ =====
	// На сайте 2 поля: серия "MP" + номер "4362782" → в БД: "MP 4362782"
	@Column(nullable = false, unique = true, length = 20)
	private String passportNumber;

	// Идентификационный номер: 5300905A014PB7
	@Column(nullable = false, unique = true, length = 20)
	private String identificationNumber;

	// ===== ЛИЧНЫЕ ДАННЫЕ =====
	// Пол: "М" или "Ж"
	@Column(nullable = false, length = 1)
	private String gender;

	// Дата рождения: 30.09.2005
	@Column(nullable = false)
	private LocalDate birthDate;

	// Дата выдачи паспорта: 30.05.2019
	@Column(nullable = false)
	private LocalDate passportIssueDate;

	// Срок действия паспорта: 30.05.2029
	@Column(nullable = false)
	private LocalDate passportExpiryDate;

	// ===== АДРЕС (прописка) =====
	@Column(nullable = false)
	private String addressStreet;   // ул пер. Кабушкино

	@Column(nullable = false)
	private String addressHouse;    // дом 31

	@Column(nullable = false)
	private String addressEntrance; // подъезд 2 (или "-")

	@Column(nullable = false)
	private String addressBuilding; // корп. - (или "-")

	@Column(nullable = false)
	private String addressApartment; // кв. 2 (или "-")

	// ===== ФОТО ПАСПОРТА (URL в Supabase Storage) =====
	@Column(length = 500)
	private String passportPhotoUrl;

	// ===== КОНТАКТ =====
	@Column(nullable = false, unique = true)
	private String phoneNumber;

	// ===== СИСТЕМА =====
	@Column(nullable = false)
	private Integer rating = 10;

	@Column(nullable = false)
	private String createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "added_by_user_id")
	private User addedBy;

	@Column(nullable = false)
	private Boolean isDeleted = false;

	// ===== ГЕТТЕРЫ И СЕТТЕРЫ =====

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getSurname() { return surname; }
	public void setSurname(String surname) { this.surname = surname; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getPatronymic() { return patronymic; }
	public void setPatronymic(String patronymic) { this.patronymic = patronymic; }

	public String getPassportNumber() { return passportNumber; }
	public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

	public String getIdentificationNumber() { return identificationNumber; }
	public void setIdentificationNumber(String identificationNumber) { this.identificationNumber = identificationNumber; }

	public String getGender() { return gender; }
	public void setGender(String gender) { this.gender = gender; }

	public LocalDate getBirthDate() { return birthDate; }
	public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

	public LocalDate getPassportIssueDate() { return passportIssueDate; }
	public void setPassportIssueDate(LocalDate passportIssueDate) { this.passportIssueDate = passportIssueDate; }

	public LocalDate getPassportExpiryDate() { return passportExpiryDate; }
	public void setPassportExpiryDate(LocalDate passportExpiryDate) { this.passportExpiryDate = passportExpiryDate; }

	public String getAddressStreet() { return addressStreet; }
	public void setAddressStreet(String addressStreet) { this.addressStreet = addressStreet; }

	public String getAddressHouse() { return addressHouse; }
	public void setAddressHouse(String addressHouse) { this.addressHouse = addressHouse; }

	public String getAddressEntrance() { return addressEntrance; }
	public void setAddressEntrance(String addressEntrance) { this.addressEntrance = addressEntrance; }

	public String getAddressBuilding() { return addressBuilding; }
	public void setAddressBuilding(String addressBuilding) { this.addressBuilding = addressBuilding; }

	public String getAddressApartment() { return addressApartment; }
	public void setAddressApartment(String addressApartment) { this.addressApartment = addressApartment; }

	public String getPassportPhotoUrl() { return passportPhotoUrl; }
	public void setPassportPhotoUrl(String passportPhotoUrl) { this.passportPhotoUrl = passportPhotoUrl; }

	public String getPhoneNumber() { return phoneNumber; }
	public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

	public Integer getRating() { return rating; }
	public void setRating(Integer rating) { this.rating = rating; }

	public String getCreatedBy() { return createdBy; }
	public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

	public User getAddedBy() { return addedBy; }
	public void setAddedBy(User addedBy) { this.addedBy = addedBy; }

	public Boolean getIsDeleted() { return isDeleted; }
	public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

	// Удобный метод: полный адрес строкой
	public String getFullAddress() {
		return addressStreet + ", " + addressHouse + ", пд." + addressEntrance + ", корп." + addressBuilding + ", кв." + addressApartment;
	}
}
