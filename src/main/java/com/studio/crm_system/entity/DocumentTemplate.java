package com.studio.crm_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Шаблон документа (Word .doc/.docx), хранится файл в Supabase Storage,
 * отредактированный HTML — в contentHtml для редактирования на сайте.
 */
@Entity
@Table(name = "document_templates")
public class DocumentTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private Long version;

	/** Отображаемое название шаблона */
	@Column(nullable = false, length = 255)
	private String name;

	/** Исходное имя файла при загрузке */
	@Column(name = "original_file_name", nullable = false, length = 255)
	private String originalFileName;

	/** Публичный URL файла в Supabase Storage (тот же bucket, что и фото паспортов) */
	@Column(name = "file_url", length = 1024)
	private String fileUrl;

	@Column(name = "file_size")
	private Long fileSize;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_id")
	private User createdBy;

	/**
	 * HTML-содержимое после редактирования в браузере (WYSIWYG).
	 * Если null — показываем контент из Word через конвертацию при открытии редактора.
	 */
	@Column(name = "content_html", columnDefinition = "TEXT")
	private String contentHtml;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getOriginalFileName() { return originalFileName; }
	public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

	public String getFileUrl() { return fileUrl; }
	public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

	public Long getFileSize() { return fileSize; }
	public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

	public User getCreatedBy() { return createdBy; }
	public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

	public String getContentHtml() { return contentHtml; }
	public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }
}
