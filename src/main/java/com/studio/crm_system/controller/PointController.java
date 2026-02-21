package com.studio.crm_system.controller;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.PointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/points")
public class PointController {

	@Autowired private PointService pointService;
	@Autowired private UserRepository userRepository;

	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = (principal instanceof UserDetails)
				? ((UserDetails) principal).getUsername()
				: principal.toString();
		return userRepository.findByLogin(username).orElse(null);
	}

	@GetMapping
	public String list(Model model) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		model.addAttribute("points", pointService.findAllActive());
		model.addAttribute("currentUser", user);
		model.addAttribute("username", user.getLogin());
		model.addAttribute("currentUserRole", user.getRole());
		return "html/points";
	}

	@PostMapping("/add")
	public String add(@RequestParam String name, @RequestParam(required = false) String address) {
		String error = pointService.create(name, address);
		if (error != null) return "redirect:/points?error=" + error;
		return "redirect:/points?success=point_added";
	}

	@PostMapping("/edit")
	public String edit(@RequestParam Long id, @RequestParam String name, @RequestParam(required = false) String address) {
		String error = pointService.update(id, name, address);
		if (error != null) return "redirect:/points?error=" + error;
		return "redirect:/points?success=point_updated";
	}

	@PostMapping("/delete")
	public String delete(@RequestParam Long id) {
		String error = pointService.softDelete(id);
		if (error != null) return "redirect:/points?error=" + error;
		return "redirect:/points?success=point_deleted";
	}
}
