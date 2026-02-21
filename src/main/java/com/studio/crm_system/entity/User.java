package com.studio.crm_system.entity;

import com.studio.crm_system.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.studio.crm_system.entity.Point;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String surname;

	@Column(nullable = false)
	private String patronymic;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, unique = true)
	private String login;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Role role;

	@Column(nullable = false)
	private String phoneNumber;

	private String instagram;
	private String facebook;
	private String telegram;
	private String vk;
	private String whatsApp;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

	/** Точка выдачи, к которой привязан сотрудник (null = без привязки / доступ ко всем для админа). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "point_id")
	private Point point;

	// МЯГКОЕ УДАЛЕНИЕ
	@Column(nullable = false)
	private Boolean isDeleted = false;

	// ЗАЩИТА ОТ БРУТФОРСА
	@Column(nullable = false)
	private Integer failedLoginAttempts = 0;

	// Аккаунт заблокирован до этого времени (null = не заблокирован)
	@Column
	private LocalDateTime lockUntil;
	
	
	public Boolean getIsDeleted() {return isDeleted;}
	public void setIsDeleted(Boolean isDeleted) {this.isDeleted = isDeleted;}

	public Integer getFailedLoginAttempts() {return failedLoginAttempts;}
	public void setFailedLoginAttempts(Integer failedLoginAttempts) {this.failedLoginAttempts = failedLoginAttempts;}

	public LocalDateTime getLockUntil() {return lockUntil;}
	public void setLockUntil(LocalDateTime lockUntil) {this.lockUntil = lockUntil;}

	// Удобный метод: заблокирован ли сейчас
	public boolean isLocked() {
		return lockUntil != null && lockUntil.isAfter(LocalDateTime.now());
	}
	public User getCreatedBy() {return createdBy;}
	public void setCreatedBy(User createdBy) {this.createdBy = createdBy;}
	public Point getPoint() {return point;}
	public void setPoint(Point point) {this.point = point;}
	public Long getId() {return id;}
	public void setId(Long id) {this.id = id;}
	public String getName() {return name;}
	public void setName(String name) {this.name = name;}
	public String getSurname() {
		return surname;}
	public void setSurname(String surname) {this.surname = surname;}
	public String getPatronymic() {return patronymic;}
	public void setPatronymic(String patronymic) {this.patronymic = patronymic;}
	public String getEmail() {return email;}
	public void setEmail(String email) {this.email = email;}
	public String getLogin() {return login;}
	public void setLogin(String login) {this.login = login;}
	public String getPassword() {return password;}
	public void setPassword(String password) {this.password = password;}
	public Role getRole() {return role;}
	public void setRole(Role role) {this.role = role;}
	public String getPhoneNumber() {return phoneNumber;}
	public void setPhoneNumber(String phoneNumber) {this.phoneNumber = phoneNumber;}
	public String getInstagram() {return instagram;}
	public void setInstagram(String instagram) {this.instagram = instagram;}
	public String getFacebook() {return facebook;}
	public void setFacebook(String facebook) {this.facebook = facebook;}
	public String getTelegram() {return telegram;}
	public void setTelegram(String telegram) {this.telegram = telegram;}
	public String getVk() {return vk;}
	public void setVk(String vk) {this.vk = vk;}
	public String getWhatsApp() {return whatsApp;}
	public void setWhatsApp(String whatsApp) {this.whatsApp = whatsApp;}

}
