package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.entity.ToolName;
import com.studio.crm_system.enums.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

	// Все НЕудалённые
	List<Equipment> findByIsDeletedFalse();

	// Только СВОБОДНЫЕ и НЕудалённые
	List<Equipment> findByStatusAndIsDeletedFalse(EquipmentStatus status);

	// Единицы конкретного раздела, НЕудалённые
	List<Equipment> findByToolNameAndIsDeletedFalse(ToolName toolName);

	// Свободные единицы конкретного раздела
	List<Equipment> findByToolNameAndStatusAndIsDeletedFalse(ToolName toolName, EquipmentStatus status);

	// По id (НЕудалённый)
	Optional<Equipment> findByIdAndIsDeletedFalse(Long id);

	// Подсчёт: всего единиц в разделе
	long countByToolNameAndIsDeletedFalse(ToolName toolName);

	// Подсчёт: свободных единиц в разделе
	long countByToolNameAndStatusAndIsDeletedFalse(ToolName toolName, EquipmentStatus status);
}
