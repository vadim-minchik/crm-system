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

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

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

	public BigDecimal getTotalAmount() { return totalAmount; }
	public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

	public RentalStatus getStatus() { return status; }
	public void setStatus(RentalStatus status) { this.status = status; }
}
