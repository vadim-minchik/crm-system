package com.studio.crm_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Отзыв о клиенте. Оценка 1–10 и текст.
 * Рейтинг клиента пересчитывается как среднее по всем отзывам.
 */
@Entity
@Table(name = "client_reviews")
public class ClientReview {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	/** Оценка от 1 до 10. */
	@Column(nullable = false)
	private Integer score;

	@Column(length = 2000)
	private String comment;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }

	public Client getClient() { return client; }
	public void setClient(Client client) { this.client = client; }

	public User getAuthor() { return author; }
	public void setAuthor(User author) { this.author = author; }

	public Integer getScore() { return score; }
	public void setScore(Integer score) { this.score = score; }

	public String getComment() { return comment; }
	public void setComment(String comment) { this.comment = comment; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
