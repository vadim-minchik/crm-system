package com.studio.crm_system.entity;

import com.studio.crm_system.enums.RentalStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "rentals")
public class Rental {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@Column(nullable = false)
	private LocalDateTime dateFrom;

	@Column(nullable = false)
	private LocalDateTime dateTo;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "rental_equipment",
			joinColumns = @JoinColumn(name = "rental_id"),
			inverseJoinColumns = @JoinColumn(name = "equipment_id"))
	private List<Equipment> equipmentList = new ArrayList<>();

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "point_id")
	private Point point;

	/** Сотрудник, который оформил прокат (из базы пользователей). */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "created_by_staff_id")
	private User createdByStaff;

	/** Сотрудник, который отдал оборудование (из базы пользователей). */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "handed_over_by_staff_id")
	private User handedOverByStaff;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	/** Доп. услуги: сумма (Br). */
	@Column(name = "additional_services_amount", precision = 12, scale = 2)
	private BigDecimal additionalServicesAmount;

	/** Доп. услуги: описание, что входит. */
	@Column(name = "additional_services_description", length = 500)
	private String additionalServicesDescription;

	/** Доставка: стоимость (Br), null или 0 — без доставки. */
	@Column(name = "delivery_amount", precision = 12, scale = 2)
	private BigDecimal deliveryAmount;

	/** Адрес доставки (улица, дом, кв. и т.д.), если есть доставка. */
	@Column(name = "delivery_address", length = 500)
	private String deliveryAddress;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private RentalStatus status = RentalStatus.ACTIVE;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Client getClient() { return client; }
	public void setClient(Client client) { this.client = client; }

	public LocalDateTime getDateFrom() { return dateFrom; }
	public void setDateFrom(LocalDateTime dateFrom) { this.dateFrom = dateFrom; }

	public LocalDateTime getDateTo() { return dateTo; }
	public void setDateTo(LocalDateTime dateTo) { this.dateTo = dateTo; }

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

	public Point getPoint() { return point; }
	public void setPoint(Point point) { this.point = point; }

	public User getCreatedByStaff() { return createdByStaff; }
	public void setCreatedByStaff(User createdByStaff) { this.createdByStaff = createdByStaff; }

	public User getHandedOverByStaff() { return handedOverByStaff; }
	public void setHandedOverByStaff(User handedOverByStaff) { this.handedOverByStaff = handedOverByStaff; }

	public BigDecimal getTotalAmount() { return totalAmount; }
	public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

	public BigDecimal getAdditionalServicesAmount() { return additionalServicesAmount; }
	public void setAdditionalServicesAmount(BigDecimal additionalServicesAmount) { this.additionalServicesAmount = additionalServicesAmount; }

	public String getAdditionalServicesDescription() { return additionalServicesDescription; }
	public void setAdditionalServicesDescription(String additionalServicesDescription) { this.additionalServicesDescription = additionalServicesDescription; }

	public BigDecimal getDeliveryAmount() { return deliveryAmount; }
	public void setDeliveryAmount(BigDecimal deliveryAmount) { this.deliveryAmount = deliveryAmount; }

	public String getDeliveryAddress() { return deliveryAddress; }
	public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

	public RentalStatus getStatus() { return status; }
	public void setStatus(RentalStatus status) { this.status = status; }
}
