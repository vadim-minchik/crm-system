package com.studio.crm_system.config;

import com.studio.crm_system.repository.PointRepository;
import com.studio.crm_system.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Для HTML-запросов добавляет в модель данные сайдбара и профиля (без вызова на JSON/fetch).
 */
@ControllerAdvice
public class CrmUserModelAdvice {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PointRepository pointRepository;

	@ModelAttribute
	public void addSidebarAndProfile(Model model, Authentication authentication, HttpServletRequest request) {
		String accept = request.getHeader("Accept");
		if (accept == null || !accept.toLowerCase().contains("text/html")) {
			return;
		}
		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getName())) {
			return;
		}
		String login = authentication.getName();
		userRepository.findByLoginAndIsDeletedFalse(login).ifPresent(user -> {
			model.addAttribute("username", user.getLogin());
			model.addAttribute("currentUserRole", user.getRole());
			model.addAttribute("crmProfileUser", user);
			model.addAttribute("profilePoints", pointRepository.findByIsDeletedFalseOrderByNameAsc());
		});
	}
}
