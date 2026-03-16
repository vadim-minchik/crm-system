package com.studio.crm_system.controller;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.DocumentTemplateRepository;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.BookingService;
import com.studio.crm_system.service.RentalDocumentService;
import com.studio.crm_system.service.RentalService;
import com.studio.crm_system.service.TemplateStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
			String fileName = "document-" + id + "-" + template.getName().replaceAll("[^a-zA-Z0-9_-]", "_") + ".docx";
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
					.contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
					.body(docx);
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
				addServ, additionalServicesDescription, delAmt, deliveryAddress, createdByStaffId, handedOverByStaffId);
		if (error != null) return "redirect:/rentals?error=" + error;
		return "redirect:/rentals?success=rental_added";
	}

	@PostMapping("/edit")
	public String edit(@RequestParam Long id,
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
		String error = rentalService.updateRental(id, from, to, totalAmount, addServ, additionalServicesDescription, delAmt, deliveryAddress, createdByStaffId, handedOverByStaffId);
		if (error != null) return "redirect:/rentals/" + id + "?error=" + error;
		return "redirect:/rentals/" + id + "?success=rental_updated";
	}

	@PostMapping("/complete")
	public String complete(@RequestParam Long id) {
		String error = rentalService.completeRental(id);
		if (error != null) return "redirect:/rentals?error=" + error;
		return "redirect:/rentals?success=rental_completed";
	}

	@PostMapping("/cancel")
	public String cancel(@RequestParam Long id) {
		String error = rentalService.cancelRental(id);
		if (error != null) return "redirect:/rentals?error=" + error;
		return "redirect:/rentals?success=rental_cancelled";
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
	public String deleteBooking(@RequestParam Long id) {
		String error = bookingService.delete(id);
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
