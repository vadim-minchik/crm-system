package com.studio.crm_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studio.crm_system.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    List<User> findByIsDeletedFalse();
    Optional<User> findByLoginAndIsDeletedFalse(String login);
    Optional<User> findByIdAndIsDeletedFalse(Long id);
}