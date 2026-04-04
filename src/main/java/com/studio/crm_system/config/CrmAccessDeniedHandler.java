package com.studio.crm_system.config;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.MenuScopeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CrmAccessDeniedHandler implements AccessDeniedHandler {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MenuScopeService menuScopeService;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
			User user = userRepository.findByLoginAndIsDeletedFalse(auth.getName()).orElse(null);
			if (user != null) {
				String ctx = request.getContextPath();
				String safe = menuScopeService.firstAccessiblePath(user);
				response.sendRedirect(ctx + safe + "?notice=no_access");
				return;
			}
		}
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}
}
