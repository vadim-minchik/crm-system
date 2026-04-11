package com.studio.crm_system.repository;

import com.studio.crm_system.entity.OwnerPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OwnerPayoutRepository extends JpaRepository<OwnerPayout, Long> {

	List<OwnerPayout> findByEquipmentOwnerIdOrderByPaidAtDesc(Long equipmentOwnerId);

	List<OwnerPayout> findByEquipmentIdOrderByPaidAtDesc(Long equipmentId);

	@Query("SELECT COALESCE(SUM(p.amount), 0) FROM OwnerPayout p WHERE p.equipmentOwner.id = :ownerId")
	BigDecimal sumAmountByEquipmentOwnerId(@Param("ownerId") Long ownerId);
}
