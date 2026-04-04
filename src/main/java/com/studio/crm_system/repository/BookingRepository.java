package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	List<Booking> findAllByOrderByCreatedAtDesc();

	List<Booking> findByDateToLessThanEqual(LocalDateTime now);

	List<Booking> findByDateToIsNull();

	@Query("SELECT b FROM Booking b JOIN b.equipmentList e WHERE e.id = :equipmentId AND b.dateTo > :after")
	List<Booking> findBookingsContainingEquipmentAndDateToAfter(@Param("equipmentId") Long equipmentId, @Param("after") LocalDateTime after);

	
	@Query("SELECT b FROM Booking b JOIN b.equipmentList e WHERE e.id = :equipmentId AND b.dateTo > :now AND (b.dateFrom IS NULL OR b.dateFrom <= :now)")
	List<Booking> findBookingsContainingEquipmentActiveAt(@Param("equipmentId") Long equipmentId, @Param("now") LocalDateTime now);

	
	@Query("SELECT b FROM Booking b WHERE b.dateTo > :now AND (b.dateFrom IS NULL OR b.dateFrom <= :now)")
	List<Booking> findBookingsStartedAndNotEnded(@Param("now") LocalDateTime now);

	
	@Query("SELECT DISTINCT b FROM Booking b JOIN b.equipmentList e WHERE e.id = :equipmentId ORDER BY b.dateTo DESC")
	List<Booking> findByEquipmentIdOrderByDateToDesc(@Param("equipmentId") Long equipmentId);
}
