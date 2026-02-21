package com.studio.crm_system.repository;

import com.studio.crm_system.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

	List<Client> findByIsDeletedFalse();

	Optional<Client> findByIdAndIsDeletedFalse(Long id);

	List<Client> findBySurnameContainingIgnoreCaseAndIsDeletedFalse(String surname);

	boolean existsByPhoneNumber(String phoneNumber);
	boolean existsByPassportNumber(String passportNumber);
	boolean existsByIdentificationNumber(String identificationNumber);
}
