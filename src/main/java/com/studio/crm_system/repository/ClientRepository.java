package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

	List<Client> findByIsDeletedFalse();

	List<Client> findByIsDeletedFalseAndBlacklistedOrderBySurnameAscNameAsc(Boolean blacklisted);

	Optional<Client> findByIdAndIsDeletedFalse(Long id);

	List<Client> findBySurnameContainingIgnoreCaseAndIsDeletedFalse(String surname);

	/** Поиск по ФИО, телефону, паспорту, идентификационному номеру, прописке. blacklistedOnly = true — только чёрный список, null — все. */
	@Query("SELECT c FROM Client c WHERE c.isDeleted = false " +
			"AND (:blacklistedOnly IS NULL OR c.blacklisted = true) " +
			"AND (LOWER(c.surname) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(c.patronymic) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(c.passportNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(c.identificationNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
			"OR LOWER(c.addressStreet) LIKE LOWER(CONCAT('%', :q, '%'))) " +
			"ORDER BY c.surname, c.name")
	List<Client> searchClients(@Param("q") String q, @Param("blacklistedOnly") Boolean blacklistedOnly);

	/** Есть ли активный (не удалённый) клиент с таким телефоном. */
	boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);

	/** Первый активный клиент с точным номером телефона (для перезвона и проверки дублей). */
	Optional<Client> findFirstByPhoneNumberAndIsDeletedFalse(String phoneNumber);
	boolean existsByPassportNumberAndIsDeletedFalse(String passportNumber);
	boolean existsByIdentificationNumberAndIsDeletedFalse(String identificationNumber);

	/** Для редактирования: другой активный клиент с таким полем (кроме текущего id). */
	boolean existsByPhoneNumberAndIsDeletedFalseAndIdNot(String phoneNumber, Long id);
	boolean existsByPassportNumberAndIsDeletedFalseAndIdNot(String passportNumber, Long id);
	boolean existsByIdentificationNumberAndIsDeletedFalseAndIdNot(String identificationNumber, Long id);
}
