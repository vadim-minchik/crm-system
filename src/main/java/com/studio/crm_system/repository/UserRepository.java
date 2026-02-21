package com.studio.crm_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studio.crm_system.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    // Этот метод Spring реализует сам — он найдет юзера по полю login
    Optional<User> findByLogin(String login);
    
    // Поиск по email для проверки уникальности
    Optional<User> findByEmail(String email);
    
    // МЯГКОЕ УДАЛЕНИЕ - получаем только НЕудаленных пользователей
    List<User> findByIsDeletedFalse();
    Optional<User> findByLoginAndIsDeletedFalse(String login);
    Optional<User> findByIdAndIsDeletedFalse(Long id);
}