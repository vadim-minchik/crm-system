package com.studio.crm_system.controller;

import com.studio.crm_system.entity.Point;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.enums.Role;
import com.studio.crm_system.repository.PointRepository;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.MenuScopeService;
import com.studio.crm_system.web.OptimisticLockSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Редактирование собственного профиля (логин для SUPER_ADMIN, остальные поля — для всех ролей).
 */
@Controller
public class ProfileController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PointRepository pointRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MenuScopeService menuScopeService;

	private static final int MAX_FIO_LENGTH = 50;
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails ud) {
			return userRepository.findByLoginAndIsDeletedFalse(ud.getUsername()).orElse(null);
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
		return login != null && !login.isEmpty() && login.matches("^[a-zA-Z0-9]+$");
	}

	private boolean isValidPassword(String password) {
		return password != null && !password.isEmpty() && password.length() >= 6;
	}

	private boolean isFioLengthValid(String value) {
		return value != null && value.length() <= MAX_FIO_LENGTH;
	}

	private LocalDate parseOptionalDate(String raw) {
		if (raw == null || raw.trim().isEmpty())
			return null;
		try {
			return LocalDate.parse(raw.trim(), DATE_FMT);
		} catch (Exception e) {
			return null;
		}
	}

	private void setOptionalStaffFieldsFromParams(String trustedPersonPhone, String passportSeries,
			String passportNumber, String identificationNumber, LocalDate passportIssueDate,
			LocalDate passportExpiryDate, String addressStreet, String addressHouse, String addressEntrance,
			String addressBuilding, String addressApartment, String socialNetwork, User to) {
		to.setTrustedPersonPhone(cleanStrict(trustedPersonPhone));
		to.setPassportSeries(passportSeries != null ? passportSeries.trim().toUpperCase() : null);
		to.setPassportNumber(passportNumber != null ? passportNumber.trim() : null);
		to.setIdentificationNumber(identificationNumber != null ? identificationNumber.trim().toUpperCase() : null);
		to.setPassportIssueDate(passportIssueDate);
		to.setPassportExpiryDate(passportExpiryDate);
		to.setAddressStreet(addressStreet != null ? addressStreet.trim() : null);
		to.setAddressHouse(addressHouse != null ? addressHouse.trim() : null);
		to.setAddressEntrance(addressEntrance != null ? addressEntrance.trim() : null);
		to.setAddressBuilding(addressBuilding != null ? addressBuilding.trim() : null);
		to.setAddressApartment(addressApartment != null ? addressApartment.trim() : null);
		to.setSocialNetwork(cleanStrict(socialNetwork));
	}

	private String safeRedirectAfterProfile(HttpServletRequest request, User actor) {
		String ref = request.getHeader("Referer");
		if (ref != null && !ref.isBlank()) {
			try {
				URI uri = URI.create(ref);
				if (uri.getHost() != null && uri.getHost().equalsIgnoreCase(request.getServerName())) {
					String path = uri.getPath();
					if (path != null && path.startsWith("/") && !path.startsWith("//") && !path.startsWith("/login")) {
						String q = uri.getRawQuery();
						return "redirect:" + path + (q != null && !q.isEmpty() ? "?" + q : "");
					}
				}
			} catch (IllegalArgumentException ignored) {
				/* ignore */
			}
		}
		return "redirect:" + menuScopeService.firstAccessiblePath(actor);
	}

	@PostMapping("/profile")
	public String updateProfile(HttpServletRequest request, HttpSession session,
			@RequestParam("id") Long id,
			@RequestParam("version") Long version,
			@RequestParam(value = "login", required = false) String loginParam,
			@RequestParam(value = "newPassword", required = false) String newPassword,
			@RequestParam("surname") String surnameRaw,
			@RequestParam("name") String nameRaw,
			@RequestParam(value = "patronymic", required = false) String patronymicRaw,
			@RequestParam("email") String emailRaw,
			@RequestParam("phoneNumber") String phoneRaw,
			@RequestParam(value = "trustedPersonPhone", required = false) String trustedPersonPhone,
			@RequestParam("pointId") Long pointId,
			@RequestParam(value = "passportSeries", required = false) String passportSeries,
			@RequestParam(value = "passportNumber", required = false) String passportNumber,
			@RequestParam(value = "identificationNumber", required = false) String identificationNumber,
			@RequestParam(value = "passportIssueDate", required = false) String passportIssueDateStr,
			@RequestParam(value = "passportExpiryDate", required = false) String passportExpiryDateStr,
			@RequestParam(value = "addressStreet", required = false) String addressStreet,
			@RequestParam(value = "addressHouse", required = false) String addressHouse,
			@RequestParam(value = "addressEntrance", required = false) String addressEntrance,
			@RequestParam(value = "addressBuilding", required = false) String addressBuilding,
			@RequestParam(value = "addressApartment", required = false) String addressApartment,
			@RequestParam(value = "socialNetwork", required = false) String socialNetwork,
			RedirectAttributes redirectAttributes) {

		User actor = getCurrentUser();
		if (actor == null || !actor.getId().equals(id)) {
			return "redirect:/login";
		}

		User dbUser = userRepository.findById(id).orElse(null);
		if (dbUser == null || Boolean.TRUE.equals(dbUser.getIsDeleted())) {
			return "redirect:/login";
		}

		if (OptimisticLockSupport.isStale(version, dbUser.getVersion())) {
			redirectAttributes.addFlashAttribute("profileError", "stale_data");
			return safeRedirectAfterProfile(request, actor);
		}

		Point point = pointRepository.findById(pointId).orElse(null);
		if (point == null || Boolean.TRUE.equals(point.getIsDeleted())) {
			redirectAttributes.addFlashAttribute("profileError", "point_invalid");
			return safeRedirectAfterProfile(request, actor);
		}

		String name = cleanCyrillic(nameRaw);
		String surname = cleanCyrillic(surnameRaw);
		if (name == null || surname == null) {
			redirectAttributes.addFlashAttribute("profileError", "bad_chars");
			return safeRedirectAfterProfile(request, actor);
		}
		if (!isFioLengthValid(name) || !isFioLengthValid(surname)) {
			redirectAttributes.addFlashAttribute("profileError", "name_too_long");
			return safeRedirectAfterProfile(request, actor);
		}
		String patronymic = (patronymicRaw == null || patronymicRaw.isBlank()) ? null : cleanCyrillic(patronymicRaw);
		if (patronymicRaw != null && !patronymicRaw.isBlank() && patronymic == null) {
			redirectAttributes.addFlashAttribute("profileError", "bad_chars");
			return safeRedirectAfterProfile(request, actor);
		}
		if (patronymic != null && !isFioLengthValid(patronymic)) {
			redirectAttributes.addFlashAttribute("profileError", "name_too_long");
			return safeRedirectAfterProfile(request, actor);
		}

		String email = cleanStrict(emailRaw);
		String phone = cleanStrict(phoneRaw);
		if (!email.equals(dbUser.getEmail())) {
			if (userRepository.findByEmailAndIsDeletedFalseAndIdNot(email, dbUser.getId()).isPresent()) {
				redirectAttributes.addFlashAttribute("profileError", "email_exists");
				return safeRedirectAfterProfile(request, actor);
			}
		}

		boolean loginChanged = false;
		if (actor.getRole() == Role.SUPER_ADMIN) {
			String cLogin = cleanStrict(loginParam);
			if (!cLogin.isEmpty() && !cLogin.equals(dbUser.getLogin())) {
				if (!isValidLogin(cLogin)) {
					redirectAttributes.addFlashAttribute("profileError", "invalid_login");
					return safeRedirectAfterProfile(request, actor);
				}
				if (userRepository.findByLoginAndIsDeletedFalseAndIdNot(cLogin, dbUser.getId()).isPresent()) {
					redirectAttributes.addFlashAttribute("profileError", "login_exists");
					return safeRedirectAfterProfile(request, actor);
				}
				dbUser.setLogin(cLogin);
				loginChanged = true;
			}
		}

		String cPass = cleanStrict(newPassword);
		if (!cPass.isEmpty()) {
			if (!isValidPassword(cPass)) {
				redirectAttributes.addFlashAttribute("profileError", "password_too_short");
				return safeRedirectAfterProfile(request, actor);
			}
			dbUser.setPassword(passwordEncoder.encode(cPass));
		}

		dbUser.setName(name);
		dbUser.setSurname(surname);
		dbUser.setPatronymic(patronymic);
		dbUser.setEmail(email);
		dbUser.setPhoneNumber(phone);
		dbUser.setPoint(point);

		LocalDate passportIssueDate = parseOptionalDate(passportIssueDateStr);
		LocalDate passportExpiryDate = parseOptionalDate(passportExpiryDateStr);
		setOptionalStaffFieldsFromParams(trustedPersonPhone, passportSeries, passportNumber, identificationNumber,
				passportIssueDate, passportExpiryDate, addressStreet, addressHouse, addressEntrance,
				addressBuilding, addressApartment, socialNetwork, dbUser);

		try {
			userRepository.save(dbUser);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("profileError", "save_failed");
			return safeRedirectAfterProfile(request, actor);
		}

		if (loginChanged) {
			SecurityContextHolder.clearContext();
			session.invalidate();
			return "redirect:/login?profile_login_changed=1";
		}

		redirectAttributes.addFlashAttribute("profileSuccess", true);
		return safeRedirectAfterProfile(request, actor);
	}
}
