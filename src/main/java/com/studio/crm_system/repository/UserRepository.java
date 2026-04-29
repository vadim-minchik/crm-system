package com.studio.crm_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.enums.Role;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    Optional<User> findByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.point WHERE u.isDeleted = false")
    List<User> findByIsDeletedFalse();
    Optional<User> findByLoginAndIsDeletedFalse(String login);
    Optional<User> findByEmailAndIsDeletedFalse(String email);
    Optional<User> findByIdAndIsDeletedFalse(Long id);
    boolean existsByRoleAndIsDeletedFalse(Role role);
    List<User> findByIsDeletedFalseAndPointIsNull();

    
    Optional<User> findByLoginAndIsDeletedFalseAndIdNot(String login, Long id);
    Optional<User> findByEmailAndIsDeletedFalseAndIdNot(String email, Long id);
}