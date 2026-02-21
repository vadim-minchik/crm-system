package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	List<Booking> findAllByOrderByCreatedAtDesc();

	List<Booking> findByDateToLessThanEqual(LocalDateTime now);

	List<Booking> findByDateToIsNull();
}
