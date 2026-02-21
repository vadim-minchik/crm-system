package com.studio.crm_system.entity;

import com.studio.crm_system.enums.RentalStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rentals")
public class Rental {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Кто берёт в прокат — ссылка на клиента */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	/** С какого числа и времени */
	@Column(nullable = false)
	private LocalDateTime dateFrom;

	/** По какое число и время */
	@Column(nullable = false)
	private LocalDateTime dateTo;

	/** Какой инструмент взят — ссылка на оборудование */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "equipment_id", nullable = false)
	private Equipment equipment;

	/** Точка выдачи, где оформили прокат (null = старая запись до введения точек). */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "point_id")
	private Point point;

	/** Итоговая сумма (BYN), рассчитанная по дням/неделям */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private RentalStatus status = RentalStatus.ACTIVE;

	// --- Геттеры и сеттеры ---

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Client getClient() { return client; }
	public void setClient(Client client) { this.client = client; }

	public LocalDateTime getDateFrom() { return dateFrom; }
	public void setDateFrom(LocalDateTime dateFrom) { this.dateFrom = dateFrom; }

	public LocalDateTime getDateTo() { return dateTo; }
	public void setDateTo(LocalDateTime dateTo) { this.dateTo = dateTo; }

	public Equipment getEquipment() { return equipment; }
	public void setEquipment(Equipment equipment) { this.equipment = equipment; }

	public Point getPoint() { return point; }
	public void setPoint(Point point) { this.point = point; }

	public BigDecimal getTotalAmount() { return totalAmount; }
	public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

	public RentalStatus getStatus() { return status; }
	public void setStatus(RentalStatus status) { this.status = status; }
}
