package com.studio.crm_system.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pre_categories")
public class PreCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(nullable = false, length = 60)
	private String name;

	@Column(length = 150)
	private String description;

	@Column(nullable = false)
	private Boolean isDeleted = false;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public Boolean getIsDeleted() { return isDeleted; }
	public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
}
