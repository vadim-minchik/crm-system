package com.studio.crm_system.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 60)
	private String name;

	@Column(length = 150)
	private String description;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "pre_category_id", nullable = false)
	private PreCategory preCategory;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_category_id")
	private Category parentCategory;

	@Column(nullable = false)
	private Boolean isDeleted = false;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public PreCategory getPreCategory() { return preCategory; }
	public void setPreCategory(PreCategory preCategory) { this.preCategory = preCategory; }

	public Boolean getIsDeleted() { return isDeleted; }
	public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

	public Category getParentCategory() { return parentCategory; }
	public void setParentCategory(Category parentCategory) { this.parentCategory = parentCategory; }
}
