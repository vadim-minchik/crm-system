package com.studio.crm_system.controller;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff")
public class StaffController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails) {
			String username = ((UserDetails) principal).getUsername();
			return userRepository.findByLogin(username).orElse(null);
		}
		return null;
	}

	private String cleanCyrillic(String input) {
		if (input == null)
			return "";
		String cleaned = input.trim().replaceAll("\\s+", "");
		return cleaned.matches("^[А-Яа-яЁё]+$") ? cleaned : null;
	}

	private String cleanStrict(String input) {
		if (input == null)
			return "";
		return input.trim().replaceAll("\\s+", "");
	}
	
	private boolean isValidLogin(String login) {
		if (login == null || login.isEmpty()) {
			return false;
		}
		return login.matches("^[a-zA-Z0-9]+$");
	}
	
	private boolean isValidPassword(String password) {
		if (password == null || password.isEmpty()) {
			return false;
		}
		return password.length() >= 6;
	}

	@GetMapping
	public String showStaff(Model model) {
		User actor = getCurrentUser();
		if (actor == null)
			return "redirect:/login";

		List<User> allUsers = userRepository.findByIsDeletedFalse();
		List<User> filteredUsers;

		if (actor.getRole() == Role.SUPER_ADMIN) {
			filteredUsers = allUsers;
		} else {
			filteredUsers = allUsers.stream().filter(u -> u.getRole() == Role.WORKER).collect(Collectors.toList());
		}

		model.addAttribute("username", actor.getLogin());
		model.addAttribute("currentUserRole", actor.getRole());
		model.addAttribute("currentUserId", actor.getId());
		model.addAttribute("allUsers", filteredUsers);

		return "html/staff";
	}

	@PostMapping("/add")
	public String addStaff(@ModelAttribute User newUser) {
		User actor = getCurrentUser();
		if (actor == null || actor.getRole() == Role.WORKER)
			return "redirect:/staff";

		String name = cleanCyrillic(newUser.getName());
		String surname = cleanCyrillic(newUser.getSurname());
		if (name == null || surname == null)
			return "redirect:/staff?error=bad_chars";

		newUser.setName(name);
		newUser.setSurname(surname);
		newUser.setPatronymic(cleanCyrillic(newUser.getPatronymic()));
		
		String login = cleanStrict(newUser.getLogin());
		String email = cleanStrict(newUser.getEmail());
		
		if (!isValidLogin(login)) {
			return "redirect:/staff?error=invalid_login";
		}
		
		if (userRepository.findByLogin(login).isPresent()) {
			return "redirect:/staff?error=login_exists";
		}
		
		if (userRepository.findByEmail(email).isPresent()) {
			return "redirect:/staff?error=email_exists";
		}
		
		newUser.setLogin(login);
		newUser.setEmail(email);
		newUser.setPhoneNumber(cleanStrict(newUser.getPhoneNumber()));

		String password = cleanStrict(newUser.getPassword());
		if (!isValidPassword(password)) {
			return "redirect:/staff?error=password_too_short";
		}
		newUser.setPassword(passwordEncoder.encode(password));

		try {
			userRepository.save(newUser);
		} catch (Exception e) {
			return "redirect:/staff?error=save_failed";
		}
		
		return "redirect:/staff?success=user_added";
	}

	@PostMapping("/edit")
	public String editStaff(@ModelAttribute User details,
			@RequestParam(value = "newPassword", required = false) String newPassword) {
		User actor = getCurrentUser();
		User dbUser = userRepository.findById(details.getId()).orElse(null);

		if (actor != null && dbUser != null) {
			if (actor.getRole() == Role.ADMIN && dbUser.getRole() != Role.WORKER)
				return "redirect:/staff";

			boolean changed = false;

			String cName = cleanCyrillic(details.getName());
			String cSurname = cleanCyrillic(details.getSurname());
			String cPatr = cleanCyrillic(details.getPatronymic());

			if (cName != null && !cName.equals(dbUser.getName())) {
				dbUser.setName(cName);
				changed = true;
			}
			if (cSurname != null && !cSurname.equals(dbUser.getSurname())) {
				dbUser.setSurname(cSurname);
				changed = true;
			}
			if (cPatr != null && !cPatr.equals(dbUser.getPatronymic())) {
				dbUser.setPatronymic(cPatr);
				changed = true;
			}

		String cEmail = cleanStrict(details.getEmail());
		String cPhone = cleanStrict(details.getPhoneNumber());
		if (!cEmail.equals(dbUser.getEmail())) {
			if (userRepository.findByEmail(cEmail).isPresent()) {
				return "redirect:/staff?error=email_exists";
			}
			dbUser.setEmail(cEmail);
			changed = true;
		}
		if (!cPhone.equals(dbUser.getPhoneNumber())) {
			dbUser.setPhoneNumber(cPhone);
			changed = true;
		}

		if (actor.getRole() == Role.SUPER_ADMIN && !actor.getId().equals(dbUser.getId())) {
			String cLogin = cleanStrict(details.getLogin());
			if (!cLogin.isEmpty() && !cLogin.equals(dbUser.getLogin())) {
				if (!isValidLogin(cLogin)) {
					return "redirect:/staff?error=invalid_login";
				}
				if (userRepository.findByLogin(cLogin).isPresent()) {
					return "redirect:/staff?error=login_exists";
				}
				dbUser.setLogin(cLogin);
				changed = true;
			}
			if (details.getRole() != null && details.getRole() != dbUser.getRole()) {
				dbUser.setRole(details.getRole());
				changed = true;
			}
		}

			String cPass = cleanStrict(newPassword);
			if (!cPass.isEmpty()) {
				if (!isValidPassword(cPass)) {
					return "redirect:/staff?error=password_too_short";
				}
				dbUser.setPassword(passwordEncoder.encode(cPass));
				changed = true;
			}

			dbUser.setTelegram(cleanStrict(details.getTelegram()));
			dbUser.setWhatsApp(cleanStrict(details.getWhatsApp()));

		if (changed) {
			try {
				userRepository.save(dbUser);
			} catch (Exception e) {
				return "redirect:/staff?error=save_failed";
			}
		}
	}
	return "redirect:/staff?success=user_updated";
}

	@PostMapping("/delete")
	public String deleteStaff(@RequestParam("id") Long id) {
		User actor = getCurrentUser();
		User target = userRepository.findById(id).orElse(null);
		if (actor != null && target != null) {
			if (actor.getId().equals(target.getId()))
				return "redirect:/staff";

			if (actor.getRole() == Role.SUPER_ADMIN && target.getRole() != Role.SUPER_ADMIN) {
				userRepository.deleteById(id);
			} else if (actor.getRole() == Role.ADMIN && target.getRole() == Role.WORKER) {
				userRepository.deleteById(id);
			}
		}
		return "redirect:/staff";
	}
}