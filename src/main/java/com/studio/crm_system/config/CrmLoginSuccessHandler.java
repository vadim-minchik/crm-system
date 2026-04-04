package com.studio.crm_system.config;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.LoginAttemptService;
import com.studio.crm_system.service.MenuScopeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CrmLoginSuccessHandler implements AuthenticationSuccessHandler {

	@Autowired
	private LoginAttemptService loginAttemptService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MenuScopeService menuScopeService;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		loginAttemptService.loginSucceeded(authentication.getName());
		String path = "/staff";
		User user = userRepository.findByLoginAndIsDeletedFalse(authentication.getName()).orElse(null);
		if (user != null)
			path = menuScopeService.firstAccessiblePath(user);
		response.sendRedirect(path);
	}
}
