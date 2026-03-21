package com.studio.crm_system.controller;

import com.studio.crm_system.entity.Point;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.PointRepository;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff")
public class StaffController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PointRepository pointRepository;

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

	private static final int MAX_FIO_LENGTH = 50;
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private boolean isFioLengthValid(String value) {
		return value != null && value.length() <= MAX_FIO_LENGTH;
	}

	private LocalDate parseOptionalDate(String raw) {
		if (raw == null || raw.trim().isEmpty()) return null;
		try {
			return LocalDate.parse(raw.trim(), DATE_FMT);
		} catch (Exception e) {
			return null;
		}
	}

	private void setOptionalStaffFields(User from, User to) {
		to.setTrustedPersonPhone(cleanStrict(from.getTrustedPersonPhone()));
		to.setPassportSeries(from.getPassportSeries() != null ? from.getPassportSeries().trim().toUpperCase() : null);
		to.setPassportNumber(from.getPassportNumber() != null ? from.getPassportNumber().trim() : null);
		to.setIdentificationNumber(from.getIdentificationNumber() != null ? from.getIdentificationNumber().trim().toUpperCase() : null);
		to.setPassportIssueDate(from.getPassportIssueDate());
		to.setPassportExpiryDate(from.getPassportExpiryDate());
		to.setAddressStreet(from.getAddressStreet() != null ? from.getAddressStreet().trim() : null);
		to.setAddressHouse(from.getAddressHouse() != null ? from.getAddressHouse().trim() : null);
		to.setAddressEntrance(from.getAddressEntrance() != null ? from.getAddressEntrance().trim() : null);
		to.setAddressBuilding(from.getAddressBuilding() != null ? from.getAddressBuilding().trim() : null);
		to.setAddressApartment(from.getAddressApartment() != null ? from.getAddressApartment().trim() : null);
		to.setSocialNetwork(cleanStrict(from.getSocialNetwork()));
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
		model.addAttribute("points", pointRepository.findByIsDeletedFalseOrderByNameAsc());

		return "html/staff";
	}

	@PostMapping("/add")
	public String addStaff(@ModelAttribute User newUser,
			@RequestParam(value = "pointId", required = false) Long pointId,
			@RequestParam(value = "passportIssueDate", required = false) String passportIssueDate,
			@RequestParam(value = "passportExpiryDate", required = false) String passportExpiryDate) {
		User actor = getCurrentUser();
		if (actor == null || actor.getRole() == Role.WORKER)
			return "redirect:/staff";

		if (pointId == null)
			return "redirect:/staff?error=point_required";
		Point point = pointRepository.findById(pointId).orElse(null);
		if (point == null || Boolean.TRUE.equals(point.getIsDeleted()))
			return "redirect:/staff?error=point_invalid";

		String name = cleanCyrillic(newUser.getName());
		String surname = cleanCyrillic(newUser.getSurname());
		if (name == null || surname == null)
			return "redirect:/staff?error=bad_chars";
		if (!isFioLengthValid(name) || !isFioLengthValid(surname))
			return "redirect:/staff?error=name_too_long";
		String patronymic = cleanCyrillic(newUser.getPatronymic());
		if (patronymic != null && !isFioLengthValid(patronymic))
			return "redirect:/staff?error=name_too_long";

		newUser.setName(name);
		newUser.setSurname(surname);
		newUser.setPatronymic(patronymic);
		
		String login = cleanStrict(newUser.getLogin());
		String email = cleanStrict(newUser.getEmail());
		
		if (!isValidLogin(login)) {
			return "redirect:/staff?error=invalid_login";
		}
		
		// Уникальность только среди неудалённых — удалённые не мешают завести такого же снова
		if (userRepository.findByLoginAndIsDeletedFalse(login).isPresent()) {
			return "redirect:/staff?error=login_exists";
		}
		if (userRepository.findByEmailAndIsDeletedFalse(email).isPresent()) {
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
		newUser.setPoint(point);

		newUser.setPassportIssueDate(parseOptionalDate(passportIssueDate));
		newUser.setPassportExpiryDate(parseOptionalDate(passportExpiryDate));
		setOptionalStaffFields(newUser, newUser);

		try {
			userRepository.save(newUser);
		} catch (Exception e) {
			return "redirect:/staff?error=save_failed";
		}

		return "redirect:/staff?success=user_added";
	}

	@PostMapping("/edit")
	public String editStaff(@ModelAttribute User details,
			@RequestParam(value = "pointId", required = false) Long pointId,
			@RequestParam(value = "newPassword", required = false) String newPassword,
			@RequestParam(value = "passportIssueDate", required = false) String passportIssueDate,
			@RequestParam(value = "passportExpiryDate", required = false) String passportExpiryDate) {
		User actor = getCurrentUser();
		User dbUser = userRepository.findById(details.getId()).orElse(null);

		if (actor != null && dbUser != null) {
			if (actor.getRole() == Role.ADMIN && dbUser.getRole() != Role.WORKER)
				return "redirect:/staff";

			if (pointId == null)
				return "redirect:/staff?error=point_required";
			Point point = pointRepository.findById(pointId).orElse(null);
			if (point == null || Boolean.TRUE.equals(point.getIsDeleted()))
				return "redirect:/staff?error=point_invalid";

			boolean changed = false;

			if (dbUser.getPoint() == null || !point.getId().equals(dbUser.getPoint().getId())) {
				dbUser.setPoint(point);
				changed = true;
			}

			String cName = cleanCyrillic(details.getName());
			String cSurname = cleanCyrillic(details.getSurname());
			String cPatr = cleanCyrillic(details.getPatronymic());
			if (cName != null && !isFioLengthValid(cName))
				return "redirect:/staff?error=name_too_long";
			if (cSurname != null && !isFioLengthValid(cSurname))
				return "redirect:/staff?error=name_too_long";
			if (cPatr != null && !isFioLengthValid(cPatr))
				return "redirect:/staff?error=name_too_long";

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
			if (userRepository.findByEmailAndIsDeletedFalseAndIdNot(cEmail, dbUser.getId()).isPresent()) {
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
				if (userRepository.findByLoginAndIsDeletedFalseAndIdNot(cLogin, dbUser.getId()).isPresent()) {
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

			details.setPassportIssueDate(parseOptionalDate(passportIssueDate));
			details.setPassportExpiryDate(parseOptionalDate(passportExpiryDate));
			setOptionalStaffFields(details, dbUser);

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