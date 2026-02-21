package com.studio.crm_system.controller;

import com.studio.crm_system.entity.User;
import com.studio.crm_system.repository.UserRepository;
import com.studio.crm_system.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/rentals")
public class RentalController {

	@Autowired private RentalService rentalService;
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

		model.addAttribute("activeRentals", rentalService.findActive());
		model.addAttribute("completedRentals", rentalService.findCompleted());
		model.addAttribute("debtorRentals", rentalService.findDebtors());
		model.addAttribute("soonDebtorRentals", rentalService.findSoonDebtors());
		model.addAttribute("clients", rentalService.findAllClients());
		model.addAttribute("equipmentOptions", rentalService.getEquipmentOptionsForSelect());
		model.addAttribute("currentUser", user);
		model.addAttribute("username", user.getLogin());
		model.addAttribute("currentUserRole", user.getRole());
		return "html/rentals";
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
		model.addAttribute("currentUser", user);
		model.addAttribute("username", user.getLogin());
		model.addAttribute("currentUserRole", user.getRole());
		return "html/rental_detail";
	}

	@PostMapping("/add")
	public String add(@RequestParam Long clientId,
	                  @RequestParam Long equipmentId,
	                  @RequestParam String dateFrom,
	                  @RequestParam String dateTo,
	                  @RequestParam(required = false) BigDecimal totalAmount) {
		LocalDateTime from = parseDateTime(dateFrom);
		LocalDateTime to = parseDateTime(dateTo);
		String error = rentalService.createRental(clientId, equipmentId, from, to, totalAmount);
		if (error != null) return "redirect:/rentals?error=" + error;
		return "redirect:/rentals?success=rental_added";
	}

	@PostMapping("/edit")
	public String edit(@RequestParam Long id,
	                  @RequestParam String dateFrom,
	                  @RequestParam String dateTo,
	                  @RequestParam(required = false) BigDecimal totalAmount) {
		LocalDateTime from = parseDateTime(dateFrom);
		LocalDateTime to = parseDateTime(dateTo);
		String error = rentalService.updateRental(id, from, to, totalAmount);
		if (error != null) return "redirect:/rentals/" + id + "?error=" + error;
		return "redirect:/rentals/" + id + "?success=rental_updated";
	}

	@PostMapping("/complete")
	public String complete(@RequestParam Long id) {
		String error = rentalService.completeRental(id);
		if (error != null) return "redirect:/rentals?error=" + error;
		return "redirect:/rentals?success=rental_completed";
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
}
