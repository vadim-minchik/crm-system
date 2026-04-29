package com.studio.crm_system.repository;

import com.studio.crm_system.entity.EquipmentOwner;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EquipmentOwnerRepository extends JpaRepository<EquipmentOwner, Long> {

	@EntityGraph(attributePaths = {"equipment", "equipment.toolName"})
	List<EquipmentOwner> findByEquipmentIdOrderBySortOrderAsc(Long equipmentId);

	@Query("SELECT DISTINCT o FROM EquipmentOwner o JOIN FETCH o.equipment e JOIN FETCH e.toolName WHERE e.isDeleted = false")
	List<EquipmentOwner> findAllForOwnerBalances();
}
