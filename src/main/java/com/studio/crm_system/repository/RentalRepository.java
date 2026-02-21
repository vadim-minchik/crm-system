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

	/** Загрузка проката с блокировкой строки (для завершения). Второй запрос ждёт и получит уже обновлённый статус. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM Rental r WHERE r.id = :id")
	Optional<Rental> findByIdForUpdate(@Param("id") Long id);

	List<Rental> findByStatusOrderByDateFromDesc(RentalStatus status);

	List<Rental> findByClientIdOrderByDateFromDesc(Long clientId);

	List<Rental> findByEquipment_IdOrderByDateFromDesc(Long equipmentId);

	/** Прокаты со статусами из списка (для фоновой перераспределения) */
	List<Rental> findByStatusInOrderByDateFromDesc(java.util.Collection<RentalStatus> statuses);

	Optional<Rental> findFirstByEquipment_IdAndStatusOrderByDateToDesc(Long equipmentId, RentalStatus status);

	Optional<Rental> findFirstByEquipment_IdAndStatusInOrderByDateToDesc(Long equipmentId, List<RentalStatus> statuses);
}
