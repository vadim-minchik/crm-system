package com.studio.crm_system.controller;

import com.studio.crm_system.entity.Client;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.ClientRepository;
import com.studio.crm_system.repository.RentalRepository;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.ClientReviewService;
import com.studio.crm_system.enums.Role;
import com.studio.crm_system.security.InputValidator;
import com.studio.crm_system.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/clients")
public class ClientController {

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private InputValidator inputValidator;

	@Autowired
	private FileStorageService storageService;

	@Autowired
	private RentalRepository rentalRepository;

	@Autowired
	private ClientReviewService clientReviewService;

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String username = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername()
				: principal.toString();
		return userRepository.findByLogin(username).orElse(null);
	}

	private LocalDate parseDate(String raw, String errorKey) {
		if (raw == null || raw.isBlank()) return null;
		try {
			return LocalDate.parse(raw.trim(), DATE_FMT);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private String addr(String value) {
		if (value == null || value.trim().isEmpty()) return "-";
		return value.trim();
	}

	private String capitalize(String s) {
		if (s == null || s.isBlank()) return s;
		String[] words = s.trim().split("\\s+");
		StringBuilder sb = new StringBuilder();
		for (String w : words) {
			if (!w.isEmpty()) {
				sb.append(Character.toUpperCase(w.charAt(0)))
				  .append(w.substring(1).toLowerCase())
				  .append(" ");
			}
		}
		return sb.toString().trim();
	}

	@GetMapping
	public String listClients(Model model,
			@RequestParam(required = false) Boolean blacklist,
			@RequestParam(required = false) String q) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		try {
			boolean showBlacklistOnly = Boolean.TRUE.equals(blacklist);
			String searchQ = q != null ? q.trim() : "";
			var clients = !searchQ.isEmpty()
				? clientRepository.searchClients(searchQ, showBlacklistOnly ? true : null)
				: (showBlacklistOnly
					? clientRepository.findByIsDeletedFalseAndBlacklistedOrderBySurnameAscNameAsc(true)
					: clientRepository.findByIsDeletedFalse());
			model.addAttribute("clients", clients);
			model.addAttribute("blacklistFilter", showBlacklistOnly);
			model.addAttribute("searchQuery", searchQ);
			model.addAttribute("currentUser", user);
			model.addAttribute("username", user.getLogin());
			model.addAttribute("currentUserRole", user.getRole());
			return "html/clients";
		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:/staff?error=clients_load_failed";
		}
	}

	@PostMapping("/add")
	public String addClient(
			@RequestParam String surname,
			@RequestParam String name,
			@RequestParam String patronymic,
			@RequestParam String passportSeries,
			@RequestParam String passportNum,
			@RequestParam String identificationNumber,
			@RequestParam String gender,
			@RequestParam String birthDate,
			@RequestParam String passportIssueDate,
			@RequestParam String passportIssuedBy,
			@RequestParam String passportExpiryDate,
			@RequestParam String addressStreet,
			@RequestParam String addressHouse,
			@RequestParam(defaultValue = "-") String addressEntrance,
			@RequestParam(defaultValue = "-") String addressBuilding,
			@RequestParam(defaultValue = "-") String addressApartment,
		@RequestParam String phoneNumber) {

		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		String issuedBy = passportIssuedBy != null ? passportIssuedBy.trim() : "";
		if (issuedBy.isEmpty()) return "redirect:/clients?error=passport_issuer_required";

		LocalDate birth = parseDate(birthDate, "birth");
		LocalDate issue = parseDate(passportIssueDate, "issue");
		LocalDate expiry = parseDate(passportExpiryDate, "expiry");

		if (birth == null) return "redirect:/clients?error=invalid_birth_date";
		if (issue == null) return "redirect:/clients?error=invalid_issue_date";
		if (expiry == null) return "redirect:/clients?error=invalid_expiry_date";

		LocalDate today = LocalDate.now();

		if (birth.isAfter(today)) return "redirect:/clients?error=birth_future";
		if (birth.isBefore(LocalDate.of(1900, 1, 1))) return "redirect:/clients?error=birth_too_old";

		if (issue.isAfter(today)) return "redirect:/clients?error=issue_future";
		if (issue.isBefore(birth)) return "redirect:/clients?error=issue_before_birth";

		if (expiry.isBefore(issue)) return "redirect:/clients?error=expiry_before_issue";
		if (expiry.isAfter(today.plusYears(15))) return "redirect:/clients?error=expiry_too_far";

		String fullPassport = (passportSeries.trim() + " " + passportNum.trim()).toUpperCase();
		String cleanPhone = inputValidator.cleanPhone(phoneNumber.trim());
		String identUpper = identificationNumber.trim().toUpperCase();

		// Уникальность только среди неудалённых — удалённые не мешают завести такого же снова
		if (clientRepository.existsByPhoneNumberAndIsDeletedFalse(cleanPhone)) {
			return "redirect:/clients?error=phone_exists";
		}
		if (clientRepository.existsByPassportNumberAndIsDeletedFalse(fullPassport)) {
			return "redirect:/clients?error=passport_exists";
		}
		if (clientRepository.existsByIdentificationNumberAndIsDeletedFalse(identUpper)) {
			return "redirect:/clients?error=ident_exists";
		}

		Client client = new Client();
		client.setSurname(capitalize(surname));
		client.setName(capitalize(name));
		client.setPatronymic(capitalize(patronymic));
		client.setPassportNumber(fullPassport);
		client.setIdentificationNumber(identUpper);
		client.setGender(gender);
		client.setBirthDate(birth);
		client.setPassportIssueDate(issue);
		client.setPassportIssuedBy(issuedBy);
		client.setPassportExpiryDate(expiry);
		client.setAddressStreet(addr(addressStreet));
		client.setAddressHouse(addr(addressHouse));
		client.setAddressEntrance(addr(addressEntrance));
		client.setAddressBuilding(addr(addressBuilding));
		client.setAddressApartment(addr(addressApartment));
		client.setPhoneNumber(cleanPhone);
		client.setRating(10);
		client.setCreatedBy(user.getLogin());
		client.setAddedBy(user);

		try {
			clientRepository.save(client);
		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:/clients?error=save_failed";
		}

		return "redirect:/clients?success=client_added";
	}

	@PostMapping("/edit")
	public String editClient(
			@RequestParam Long id,
			@RequestParam String surname,
			@RequestParam String name,
			@RequestParam String patronymic,
			@RequestParam String passportSeries,
			@RequestParam String passportNum,
			@RequestParam String identificationNumber,
			@RequestParam String gender,
			@RequestParam String birthDate,
			@RequestParam String passportIssueDate,
			@RequestParam String passportIssuedBy,
			@RequestParam String passportExpiryDate,
			@RequestParam String addressStreet,
			@RequestParam String addressHouse,
			@RequestParam(defaultValue = "-") String addressEntrance,
			@RequestParam(defaultValue = "-") String addressBuilding,
			@RequestParam(defaultValue = "-") String addressApartment,
			@RequestParam String phoneNumber,
			@RequestParam(required = false) Boolean blacklisted) {

		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		String issuedBy = passportIssuedBy != null ? passportIssuedBy.trim() : "";
		if (issuedBy.isEmpty()) return "redirect:/clients?error=passport_issuer_required";

		Client dbClient = clientRepository.findById(id).orElse(null);
		if (dbClient == null) return "redirect:/clients?error=client_not_found";

		if (user.getRole() == Role.WORKER && !user.getLogin().equals(dbClient.getCreatedBy())) {
			return "redirect:/clients?error=access_denied";
		}

		LocalDate birth = parseDate(birthDate, "birth");
		LocalDate issue = parseDate(passportIssueDate, "issue");
		LocalDate expiry = parseDate(passportExpiryDate, "expiry");

		if (birth == null) return "redirect:/clients?error=invalid_birth_date";
		if (issue == null) return "redirect:/clients?error=invalid_issue_date";
		if (expiry == null) return "redirect:/clients?error=invalid_expiry_date";

		LocalDate today = LocalDate.now();
		if (birth.isAfter(today)) return "redirect:/clients?error=birth_future";
		if (birth.isBefore(LocalDate.of(1900, 1, 1))) return "redirect:/clients?error=birth_too_old";
		if (issue.isAfter(today)) return "redirect:/clients?error=issue_future";
		if (issue.isBefore(birth)) return "redirect:/clients?error=issue_before_birth";
		if (expiry.isBefore(issue)) return "redirect:/clients?error=expiry_before_issue";
		if (expiry.isAfter(today.plusYears(15))) return "redirect:/clients?error=expiry_too_far";

		String fullPassport = (passportSeries.trim() + " " + passportNum.trim()).toUpperCase();
		String cleanPhone = inputValidator.cleanPhone(phoneNumber.trim());
		String identUpper = identificationNumber.trim().toUpperCase();

		// Уникальность только среди неудалённых; при редактировании — кроме текущего клиента
		if (clientRepository.existsByPhoneNumberAndIsDeletedFalseAndIdNot(cleanPhone, dbClient.getId())) {
			return "redirect:/clients?error=phone_exists";
		}
		if (clientRepository.existsByPassportNumberAndIsDeletedFalseAndIdNot(fullPassport, dbClient.getId())) {
			return "redirect:/clients?error=passport_exists";
		}
		if (clientRepository.existsByIdentificationNumberAndIsDeletedFalseAndIdNot(identUpper, dbClient.getId())) {
			return "redirect:/clients?error=ident_exists";
		}

		dbClient.setSurname(capitalize(surname));
		dbClient.setName(capitalize(name));
		dbClient.setPatronymic(capitalize(patronymic));
		dbClient.setPassportNumber(fullPassport);
		dbClient.setIdentificationNumber(identUpper);
		dbClient.setGender(gender);
		dbClient.setBirthDate(birth);
		dbClient.setPassportIssueDate(issue);
		dbClient.setPassportIssuedBy(issuedBy);
		dbClient.setPassportExpiryDate(expiry);
		dbClient.setAddressStreet(addr(addressStreet));
		dbClient.setAddressHouse(addr(addressHouse));
		dbClient.setAddressEntrance(addr(addressEntrance));
		dbClient.setAddressBuilding(addr(addressBuilding));
		dbClient.setAddressApartment(addr(addressApartment));
		dbClient.setPhoneNumber(cleanPhone);
		dbClient.setBlacklisted(Boolean.TRUE.equals(blacklisted));
		// Рейтинг не редактируется вручную — считается по отзывам

		try {
			clientRepository.save(dbClient);
		} catch (Exception e) {
			return "redirect:/clients?error=save_failed";
		}

		return "redirect:/clients?success=client_updated";
	}

	@GetMapping("/{id}")
	public String clientDetail(@PathVariable Long id, Model model) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		Client client = clientRepository.findById(id).orElse(null);
		if (client == null || client.getIsDeleted()) return "redirect:/clients?error=client_not_found";

		model.addAttribute("client", client);
		model.addAttribute("rentals", rentalRepository.findByClientIdOrderByDateFromDesc(id));
		model.addAttribute("reviews", clientReviewService.findByClientIdOrderByCreatedAtDesc(id));
		model.addAttribute("currentUser", user);
		model.addAttribute("currentUserRole", user.getRole());
		return "html/client_detail";
	}

	@PostMapping("/{id}/toggle-blacklist")
	public String toggleBlacklist(@PathVariable Long id) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		Client client = clientRepository.findById(id).orElse(null);
		if (client == null || client.getIsDeleted()) return "redirect:/clients?error=client_not_found";

		if (user.getRole() == Role.WORKER && !user.getLogin().equals(client.getCreatedBy())) {
			return "redirect:/clients?error=access_denied";
		}

		client.setBlacklisted(Boolean.FALSE.equals(client.getBlacklisted()));
		clientRepository.save(client);
		return "redirect:/clients/" + id + "?success=" + (client.getBlacklisted() ? "blacklisted" : "removed_from_blacklist");
	}

	@PostMapping("/{id}/reviews/add")
	public String addReview(@PathVariable Long id,
			@RequestParam int score,
			@RequestParam(required = false, defaultValue = "") String comment) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		Client client = clientRepository.findById(id).orElse(null);
		if (client == null || client.getIsDeleted()) {
			return "redirect:/clients?error=client_not_found";
		}

		try {
			clientReviewService.addReview(id, user, score, comment);
		} catch (IllegalArgumentException e) {
			return "redirect:/clients/" + id + "?error=invalid_score";
		}
		return "redirect:/clients/" + id + "?success=review_added";
	}

	@PostMapping("/{id}/reviews/delete")
	public String deleteReview(@PathVariable Long id, @RequestParam Long reviewId) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		var review = clientReviewService.findById(reviewId).orElse(null);
		if (review == null || !review.getClient().getId().equals(id)) {
			return "redirect:/clients/" + id + "?error=review_not_found";
		}

		// Работник может удалять только свои отзывы; админ — любые
		if (user.getRole() == Role.WORKER) {
			if (review.getAuthor() == null || !review.getAuthor().getId().equals(user.getId())) {
				return "redirect:/clients/" + id + "?error=review_delete_denied";
			}
		}

		clientReviewService.deleteReview(reviewId);
		return "redirect:/clients/" + id + "?success=review_deleted";
	}

	@PostMapping("/{id}/upload-photo")
	public String uploadPhoto(@PathVariable Long id,
	                          @RequestParam("photo") MultipartFile photo) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		Client client = clientRepository.findById(id).orElse(null);
		if (client == null) return "redirect:/clients?error=client_not_found";

		if (photo.isEmpty()) return "redirect:/clients/" + id + "?error=no_file";

		String contentType = photo.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			return "redirect:/clients/" + id + "?error=not_image";
		}

		try {
			if (client.getPassportPhotoUrl() != null) {
				storageService.deleteByUrl(client.getPassportPhotoUrl());
			}
			String url = storageService.uploadPassportPhoto(photo, id);
			client.setPassportPhotoUrl(url);
			clientRepository.save(client);
		} catch (Exception e) {
			e.printStackTrace();
			return "redirect:/clients/" + id + "?error=upload_failed";
		}

		return "redirect:/clients/" + id + "?success=photo_uploaded";
	}

	@PostMapping("/{id}/delete-photo")
	public String deletePhoto(@PathVariable Long id) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		Client client = clientRepository.findById(id).orElse(null);
		if (client == null) return "redirect:/clients";

		if (client.getPassportPhotoUrl() != null) {
			storageService.deleteByUrl(client.getPassportPhotoUrl());
			client.setPassportPhotoUrl(null);
			clientRepository.save(client);
		}

		return "redirect:/clients/" + id + "?success=photo_deleted";
	}

	@PostMapping("/delete")
	public String deleteClient(@RequestParam Long id) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		Client dbClient = clientRepository.findById(id).orElse(null);
		if (dbClient == null) return "redirect:/clients?error=client_not_found";

		if (user.getRole() == Role.WORKER && !user.getLogin().equals(dbClient.getCreatedBy())) {
			return "redirect:/clients?error=access_denied";
		}

		try {
			dbClient.setIsDeleted(true);
			clientRepository.save(dbClient);
		} catch (Exception e) {
			return "redirect:/clients?error=delete_failed";
		}

		return "redirect:/clients?success=client_deleted";
	}
}
