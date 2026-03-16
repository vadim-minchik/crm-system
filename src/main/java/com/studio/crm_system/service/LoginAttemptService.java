package com.studio.crm_system.service;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void loginFailed(String login) {
        userRepository.findByLoginAndIsDeletedFalse(login).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_ATTEMPTS) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                System.out.println("[Security] Аккаунт '" + login + "' заблокирован на " + LOCK_DURATION_MINUTES + " мин. после " + attempts + " неудачных попыток.");
            }

            userRepository.save(user);
        });
    }

    @Transactional
    public void loginSucceeded(String login) {
        userRepository.findByLoginAndIsDeletedFalse(login).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getLockUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockUntil(null);
                userRepository.save(user);
            }
        });
    }

    public boolean isLocked(String login) {
        return userRepository.findByLoginAndIsDeletedFalse(login)
                .map(User::isLocked)
                .orElse(false);
    }

    public long minutesUntilUnlock(String login) {
        return userRepository.findByLoginAndIsDeletedFalse(login)
                .filter(User::isLocked)
                .map(user -> java.time.Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes() + 1)
                .orElse(0L);
    }
}
