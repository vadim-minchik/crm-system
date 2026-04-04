package com.studio.crm_system.repository;

import com.studio.crm_system.entity.CallbackRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CallbackRequestRepository extends JpaRepository<CallbackRequest, Long> {

	@Query("SELECT c FROM CallbackRequest c LEFT JOIN FETCH c.equipment LEFT JOIN FETCH c.creator ORDER BY c.createdAt DESC")
	List<CallbackRequest> findAllWithEquipmentOrderByCreatedAtDesc();
}
