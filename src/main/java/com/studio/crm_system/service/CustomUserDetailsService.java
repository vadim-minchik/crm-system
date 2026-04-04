package com.studio.crm_system.service;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuScopeService menuScopeService;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByLoginAndIsDeletedFalse(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + login));

        if (user.isLocked()) {
            long minutes = Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes() + 1;
            throw new LockedException("Аккаунт заблокирован. Повторите через " + minutes + " мин.");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole().name()));
        authorities.addAll(menuScopeService.menuAuthorities(user));

        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),
                user.getPassword(),
                authorities
        );
    }
}
