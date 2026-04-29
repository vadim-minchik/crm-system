package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Equipment;
import com.studio.crm_system.entity.ToolName;
import com.studio.crm_system.enums.EquipmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

	List<Equipment> findByIsDeletedFalse();

	List<Equipment> findByStatusAndIsDeletedFalse(EquipmentStatus status);

	List<Equipment> findByStatusInAndIsDeletedFalse(Collection<EquipmentStatus> statuses);

	@EntityGraph("Equipment.withOwners")
	List<Equipment> findByToolNameAndIsDeletedFalse(ToolName toolName);

	@EntityGraph("Equipment.withOwners")
	Page<Equipment> findByToolNameAndIsDeletedFalse(ToolName toolName, Pageable pageable);

	List<Equipment> findByToolNameAndStatusAndIsDeletedFalse(ToolName toolName, EquipmentStatus status);

	@EntityGraph("Equipment.forUnitDetail")
	Optional<Equipment> findByIdAndIsDeletedFalse(Long id);

	boolean existsBySerialNumberAndIsDeletedFalse(String serialNumber);
	boolean existsBySerialNumberAndIsDeletedFalseAndIdNot(String serialNumber, Long id);

	long countByToolNameAndIsDeletedFalse(ToolName toolName);

	long countByToolNameAndStatusAndIsDeletedFalse(ToolName toolName, EquipmentStatus status);
}
