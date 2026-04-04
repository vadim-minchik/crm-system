package com.studio.crm_system.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "clients")
public class Client {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(nullable = false)
	private String surname;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String patronymic;

	
	@Column(nullable = false, length = 20)
	private String passportNumber;

	
	@Column(nullable = false, length = 20)
	private String identificationNumber;

	@Column(nullable = false, length = 1)
	private String gender;

	@Column(nullable = false)
	private LocalDate birthDate;

	@Column(nullable = false)
	private LocalDate passportIssueDate;

	
	@Column(length = 500)
	private String passportIssuedBy;

	@Column(nullable = false)
	private LocalDate passportExpiryDate;

	@Column(nullable = false)
	private String addressStreet;

	@Column(nullable = false)
	private String addressHouse;

	@Column(nullable = false)
	private String addressEntrance;

	@Column(nullable = false)
	private String addressBuilding;

	@Column(nullable = false)
	private String addressApartment;

	@Column(length = 500)
	private String passportPhotoUrl;

	
	@Column(nullable = false)
	private String phoneNumber;

	
	@Column(nullable = false)
	private Integer rating = 10;

	@Column(nullable = false)
	private String createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "added_by_user_id")
	private User addedBy;

	@Column(nullable = false)
	private Boolean isDeleted = false;

	
	@Column(nullable = false)
	private Boolean blacklisted = false;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }

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

	public String getPassportIssuedBy() { return passportIssuedBy; }
	public void setPassportIssuedBy(String passportIssuedBy) { this.passportIssuedBy = passportIssuedBy; }

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

	public Boolean getBlacklisted() { return blacklisted; }
	public void setBlacklisted(Boolean blacklisted) { this.blacklisted = blacklisted; }

	public String getFullAddress() {
		return addressStreet + ", " + addressHouse + ", пд." + addressEntrance + ", корп." + addressBuilding + ", кв." + addressApartment;
	}
}
