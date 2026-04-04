package com.studio.crm_system.config;

import com.studio.crm_system.service.CustomUserDetailsService;
import com.studio.crm_system.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private CrmLoginSuccessHandler crmLoginSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.userDetailsService(userDetailsService)
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/login", "/logout").permitAll()
                        .requestMatchers("/staff/**").hasAuthority("MENU_STAFF")
                        .requestMatchers("/callbacks/**").hasAuthority("MENU_CALLBACKS")
                        .requestMatchers("/clients/**").hasAuthority("MENU_CLIENTS")
                        .requestMatchers("/inventory/**").hasAuthority("MENU_INVENTORY")
                        .requestMatchers("/points/**").hasAuthority("MENU_POINTS")
                        .requestMatchers("/rentals/**").hasAuthority("MENU_RENTALS")
                        .requestMatchers("/documents/**").hasAuthority("MENU_DOCUMENTS")
                        .requestMatchers("/statistics/**").hasAuthority("MENU_STATISTICS")
                        .anyRequest().authenticated())
                .formLogin((form) -> form
                        .loginPage("/login")
                        .successHandler(crmLoginSuccessHandler)
                        .failureHandler(authenticationFailureHandler())
                        .permitAll())
                .logout((logout) -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .maxSessionsPreventsLogin(true)
                                .sessionRegistry(sessionRegistry)));

        return http.build();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) -> {
            if (exception instanceof SessionAuthenticationException) {
                response.sendRedirect("/login?error=session_busy");
                return;
            }
            String login = request.getParameter("username");
            if (login != null && !login.isBlank()) {
                loginAttemptService.loginFailed(login);

                if (loginAttemptService.isLocked(login)) {
                    long minutes = loginAttemptService.minutesUntilUnlock(login);
                    response.sendRedirect("/login?error=locked&minutes=" + minutes);
                    return;
                }
            }
            response.sendRedirect("/login?error=bad_credentials");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
