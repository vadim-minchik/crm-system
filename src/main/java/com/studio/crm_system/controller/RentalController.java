package com.studio.crm_system.controller;

import com.studio.crm_system.entity.Rental;
import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.DocumentTemplateRepository;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.BookingService;
import com.studio.crm_system.service.RentalDocumentService;
import com.studio.crm_system.service.RentalService;
import com.studio.crm_system.service.TemplateStorageService;
import com.studio.crm_system.util.ClientPassportChecks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/rentals")
public class RentalController {

	@Autowired private RentalService rentalService;
	@Autowired private BookingService bookingService;
	@Autowired private UserRepository userRepository;
	@Autowired private DocumentTemplateRepository documentTemplateRepository;
	@Autowired private RentalDocumentService rentalDocumentService;
	@Autowired private TemplateStorageService templateStorageService;

	/** Убирает символы, недопустимые в имени файла Windows; кириллицу сохраняет. */
	private static String filenameSafeSegment(String s) {
		if (s == null || s.isBlank()) {
			return "";
		}
		String t = s.trim().replaceAll("[<>:\"/\\\\|?*\\x00-\\x1f]+", "_");
		t = t.replaceAll("_{2,}", "_").replaceAll("^_+|_+$", "");
		return t;
	}

	/**
	 * Имя файла: номер проката, дата начала, фамилия клиента, краткое имя шаблона (латиница/цифры).
	 */
	private static String buildRentalDocxDownloadFilename(Rental rental, String templateName) {
		String datePart = rental.getDateFrom() != null
				? rental.getDateFrom().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
				: "unknown";
		String surnamePart = "";
		if (rental.getClient() != null && rental.getClient().getSurname() != null) {
			surnamePart = filenameSafeSegment(rental.getClient().getSurname());
		}
		if (surnamePart.isBlank()) {
			surnamePart = "client";
		}
		if (surnamePart.length() > 40) {
			surnamePart = surnamePart.substring(0, 40);
		}
		String slug = templateName != null ? templateName : "document";
		slug = slug.replaceAll("[^a-zA-Z0-9_-]+", "_").replaceAll("^_+|_+$", "");
		if (slug.isBlank()) {
			slug = "document";
		}
		if (slug.length() > 45) {
			slug = slug.substring(0, 45);
		}
		return "prokat-" + rental.getId() + "-ot-" + datePart + "-" + surnamePart + "-" + slug + ".docx";
	}

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

		model.addAttribute("activeRentals", nullToEmpty(rentalService.findActive()));
		model.addAttribute("awaitingDeliveryRentals", nullToEmpty(rentalService.findAwaitingDelivery()));
		model.addAttribute("completedRentals", nullToEmpty(rentalService.findCompleted()));
		model.addAttribute("debtorRentals", nullToEmpty(rentalService.findDebtors()));
		model.addAttribute("soonDebtorRentals", nullToEmpty(rentalService.findSoonDebtors()));
		model.addAttribute("cancelledRentals", nullToEmpty(rentalService.findCancelled()));
		model.addAttribute("bookings", nullToEmpty(bookingService.findAll()));
		model.addAttribute("equipmentOptionsForBooking", nullToEmpty(rentalService.getEquipmentOptionsForBooking()));
		model.addAttribute("clients", nullToEmpty(rentalService.findAllClients()));
		model.addAttribute("equipmentOptions", nullToEmpty(rentalService.getEquipmentOptionsForSelect()));
		model.addAttribute("documentTemplates", nullToEmpty(documentTemplateRepository.findAllByOrderByCreatedAtDesc()));
		model.addAttribute("staffList", nullToEmpty(userRepository.findByIsDeletedFalse()));
		model.addAttribute("currentUser", user);
		model.addAttribute("username", user.getLogin());
		model.addAttribute("currentUserRole", user.getRole());
		return "html/rentals";
	}

	/**
	 * Сформировать документ по прокату и скачать как .docx (исходный шаблон с подставленными данными).
	 */
	@GetMapping(value = "/{id}/document/docx", produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
	public ResponseEntity<byte[]> getRentalDocumentAsDocx(@PathVariable Long id, @RequestParam Long templateId) {
		if (getCurrentUser() == null)
			return ResponseEntity.status(401).build();

		var rental = rentalService.findByIdForDocument(id).orElse(null);
		if (rental == null)
			return ResponseEntity.notFound().build();

		if (rental.getClient() != null && !ClientPassportChecks.isPassportValidToday(rental.getClient())) {
			String msg = "Паспорт клиента просрочен. Обновите данные клиента в базе (срок действия паспорта), затем сформируйте документ снова.";
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
					.body(msg.getBytes(StandardCharsets.UTF_8));
		}

		var template = documentTemplateRepository.findById(templateId).orElse(null);
		if (template == null || template.getFileUrl() == null || template.getFileUrl().isBlank())
			return ResponseEntity.notFound().build();

		byte[] templateBytes = templateStorageService.downloadByStoredUrl(template.getFileUrl());
		if (templateBytes == null || templateBytes.length < 22)
			return ResponseEntity.notFound().build();

		try {
			byte[] docx = rentalDocumentService.fillTemplateDocx(templateBytes, rental);
			if (docx == null)
				return ResponseEntity.notFound().build();
			String fileName = buildRentalDocxDownloadFilename(rental, template.getName());
			ContentDisposition disposition = ContentDisposition.attachment()
					.filename(fileName, StandardCharsets.UTF_8)
					.build();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentDisposition(disposition);
			headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
			return new ResponseEntity<>(docx, headers, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}

	@GetMapping("/{id}")
	public String rentalDetail(@PathVariable Long id, Model model) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		var rental = rentalService.findById(id).orElse(null);
		if (rental == null) return "redirect:/rentals?error=not_found";

		model.addAttribute("rental", rental);
		model.addAttribute("rentalDateFromInput", rental.getDateFrom().format(DATETIME_FMT));
		model.addAttribute("rentalDateToInput", rental.getDateTo().format(DATETIME_FMT));
		model.addAttribute("deliveryLeadDescription", rentalDocumentService.buildDeliveryLeadDescriptionForUi(rental));
		model.addAttribute("staffList", nullToEmpty(userRepository.findByIsDeletedFalse()));
		model.addAttribute("currentUser", user);
		model.addAttribute("username", user.getLogin());
		model.addAttribute("currentUserRole", user.getRole());
		return "html/rental_detail";
	}

	@PostMapping("/add")
	public String add(@RequestParam Long clientId,
	                  @RequestParam List<Long> equipmentId,
	                  @RequestParam String dateFrom,
	                  @RequestParam String dateTo,
	                  @RequestParam(required = false) String totalAmount,
	                  @RequestParam(required = false) String additionalServicesAmount,
	                  @RequestParam(required = false) String additionalServicesDescription,
	                  @RequestParam(required = false) String deliveryAmount,
	                  @RequestParam(required = false) String deliveryAddress,
	                  @RequestParam(required = false, defaultValue = "false") boolean deliveryRequested,
	                  @RequestParam(required = false) Long handedOverByStaffId) {
		User currentUser = getCurrentUser();
		LocalDateTime from = parseDateTime(dateFrom);
		LocalDateTime to = parseDateTime(dateTo);
		if (equipmentId == null || equipmentId.isEmpty()) {
			return "redirect:/rentals?error=equipment_required";
		}
		BigDecimal manualTotal = parseDecimal(totalAmount);
		BigDecimal addServ = parseDecimal(additionalServicesAmount);
		BigDecimal delAmt = parseDecimal(deliveryAmount);
		Long createdByStaffId = currentUser != null ? currentUser.getId() : null;
		String error = rentalService.createRentals(clientId, equipmentId, from, to, manualTotal,
				addServ, additionalServicesDescription, delAmt, deliveryAddress, deliveryRequested, createdByStaffId, handedOverByStaffId);
		if ("awaiting_delivery".equals(error)) return "redirect:/rentals?success=rental_awaiting_delivery";
		if (error != null) return "redirect:/rentals?error=" + error;
		return "redirect:/rentals?success=rental_added";
	}

	@PostMapping("/edit")
	public String edit(@RequestParam Long id,
	                  @RequestParam Long version,
	                  @RequestParam String dateFrom,
	                  @RequestParam String dateTo,
	                  @RequestParam(required = false) BigDecimal totalAmount,
	                  @RequestParam(required = false) String additionalServicesAmount,
	                  @RequestParam(required = false) String additionalServicesDescription,
	                  @RequestParam(required = false) String deliveryAmount,
	                  @RequestParam(required = false) String deliveryAddress,
	                  @RequestParam(required = false) Long createdByStaffId,
	                  @RequestParam(required = false) Long handedOverByStaffId) {
		LocalDateTime from = parseDateTime(dateFrom);
		LocalDateTime to = parseDateTime(dateTo);
		BigDecimal addServ = parseDecimal(additionalServicesAmount);
		BigDecimal delAmt = parseDecimal(deliveryAmount);
		String error = rentalService.updateRental(id, version, from, to, totalAmount, addServ, additionalServicesDescription, delAmt, deliveryAddress, createdByStaffId, handedOverByStaffId);
		if (error != null) return "redirect:/rentals/" + id + "?error=" + error;
		return "redirect:/rentals/" + id + "?success=rental_updated";
	}

	@PostMapping("/complete")
	public String complete(@RequestParam Long id, @RequestParam Long version) {
		String error = rentalService.completeRental(id, version);
		if (error != null) return "redirect:/rentals?error=" + error;
		return "redirect:/rentals?success=rental_completed";
	}

	@PostMapping("/cancel")
	public String cancel(@RequestParam Long id, @RequestParam Long version) {
		String error = rentalService.cancelRental(id, version);
		if (error != null) return "redirect:/rentals?error=" + error;
		return "redirect:/rentals?success=rental_cancelled";
	}

	@PostMapping("/delivered")
	public String markDelivered(@RequestParam Long id, @RequestParam Long version, @RequestParam(required = false) String redirectTo) {
		User current = getCurrentUser();
		if (current == null) return "redirect:/login";
		String error = rentalService.markDelivered(id, current.getId(), version);
		if (error != null) {
			if ("detail".equals(redirectTo)) return "redirect:/rentals/" + id + "?error=" + error;
			return "redirect:/rentals?error=" + error;
		}
		if ("detail".equals(redirectTo)) return "redirect:/rentals/" + id + "?success=delivered";
		return "redirect:/rentals?success=delivered";
	}

	@PostMapping("/booking/add")
	public String addBooking(@RequestParam String phoneNumber,
	                        @RequestParam List<Long> equipmentId,
	                        @RequestParam(required = false) String dateFrom,
	                        @RequestParam String dateTo,
	                        @RequestParam(required = false) String comment) {
		LocalDateTime from = (dateFrom != null && !dateFrom.isBlank()) ? parseDateTime(dateFrom) : null;
		LocalDateTime to = parseDateTime(dateTo);
		String error;
		if (equipmentId == null || equipmentId.isEmpty()) {
			error = "equipment_required";
		} else if (equipmentId.size() == 1) {
			error = bookingService.create(phoneNumber, equipmentId.get(0), from, to, comment);
		} else {
			error = bookingService.createBatch(phoneNumber, equipmentId, from, to, comment);
		}
		if (error != null) return "redirect:/rentals?error=" + error;
		return "redirect:/rentals?success=booking_added";
	}

	@GetMapping("/booking/{id}")
	public String bookingDetail(@PathVariable Long id, Model model) {
		User user = getCurrentUser();
		if (user == null) return "redirect:/login";

		var booking = bookingService.findById(id).orElse(null);
		if (booking == null) return "redirect:/rentals?error=not_found";

		model.addAttribute("booking", booking);
		model.addAttribute("currentUser", user);
		model.addAttribute("username", user.getLogin());
		model.addAttribute("currentUserRole", user.getRole());
		return "html/booking_detail";
	}

	@PostMapping("/booking/delete")
	public String deleteBooking(@RequestParam Long id, @RequestParam Long version) {
		String error = bookingService.delete(id, version);
		if (error != null) return "redirect:/rentals?error=" + error;
		return "redirect:/rentals?success=booking_deleted";
	}

	private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

	private LocalDateTime parseDateTime(String raw) {
		if (raw == null || raw.isBlank()) return null;
		try {
			return LocalDateTime.parse(raw.trim(), DATETIME_FMT);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static <T> List<T> nullToEmpty(List<T> list) {
		return list != null ? list : Collections.emptyList();
	}

	private BigDecimal parseDecimal(String raw) {
		if (raw == null || raw.isBlank()) return null;
		String normalized = raw.trim().replace(',', '.');
		try {
			BigDecimal value = new BigDecimal(normalized);
			return value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
