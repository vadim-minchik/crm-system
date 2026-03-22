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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.userDetailsService(userDetailsService)
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/login", "/logout").permitAll()
                        .requestMatchers("/points/**", "/documents/**", "/statistics/**")
                                .hasAnyAuthority("ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated())
                .formLogin((form) -> form
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler())
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
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            loginAttemptService.loginSucceeded(authentication.getName());
            response.sendRedirect("/staff");
        };
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
