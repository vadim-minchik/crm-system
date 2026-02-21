package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Rental;
import com.studio.crm_system.enums.RentalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

	List<Rental> findAllByOrderByDateFromDesc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM Rental r WHERE r.id = :id")
	Optional<Rental> findByIdForUpdate(@Param("id") Long id);

	List<Rental> findByStatusOrderByDateFromDesc(RentalStatus status);

	List<Rental> findByClientIdOrderByDateFromDesc(Long clientId);

	@Query("SELECT r FROM Rental r LEFT JOIN FETCH r.equipmentList WHERE r.id = :id")
	Optional<Rental> findByIdWithEquipment(@Param("id") Long id);

	@Query("SELECT r FROM Rental r JOIN r.equipmentList e WHERE e.id = :equipmentId ORDER BY r.dateFrom DESC")
	List<Rental> findByEquipmentIdOrderByDateFromDesc(@Param("equipmentId") Long equipmentId);

	List<Rental> findByStatusInOrderByDateFromDesc(java.util.Collection<RentalStatus> statuses);

	@Query("SELECT r FROM Rental r JOIN r.equipmentList e WHERE e.id = :equipmentId AND r.status = :status ORDER BY r.dateTo DESC")
	Optional<Rental> findFirstByEquipmentIdAndStatusOrderByDateToDesc(@Param("equipmentId") Long equipmentId, @Param("status") RentalStatus status);

	@Query("SELECT r FROM Rental r JOIN r.equipmentList e WHERE e.id = :equipmentId AND r.status IN :statuses ORDER BY r.dateTo DESC")
	Optional<Rental> findFirstByEquipmentIdAndStatusInOrderByDateToDesc(@Param("equipmentId") Long equipmentId, @Param("statuses") List<RentalStatus> statuses);
}
